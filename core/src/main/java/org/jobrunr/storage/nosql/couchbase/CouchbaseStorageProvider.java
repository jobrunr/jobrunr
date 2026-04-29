package org.jobrunr.storage.nosql.couchbase;

import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.core.msg.kv.DurabilityLevel;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.Scope;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.*;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;
import com.couchbase.client.java.query.QueryScanConsistency;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.JobListVersioner;
import org.jobrunr.jobs.JobVersioner;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.AbstractStorageProvider;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.RecurringJobsResult;
import org.jobrunr.storage.ServerTimedOutException;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.storage.navigation.AmountRequest;
import org.jobrunr.storage.navigation.OffsetBasedPageRequest;
import org.jobrunr.storage.navigation.OrderTerm;
import org.jobrunr.storage.nosql.NoSqlStorageProvider;
import org.jobrunr.storage.nosql.couchbase.mapper.BackgroundJobServerStatusDocumentMapper;
import org.jobrunr.storage.nosql.couchbase.mapper.JobDocumentMapper;
import org.jobrunr.storage.nosql.couchbase.mapper.MetadataDocumentMapper;
import org.jobrunr.utils.resilience.RateLimiter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.jobrunr.jobs.states.StateName.areAllStateNames;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;
import static org.jobrunr.storage.StorageProviderUtils.Jobs;
import static org.jobrunr.storage.StorageProviderUtils.Metadata;
import static org.jobrunr.storage.StorageProviderUtils.RecurringJobs;
import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.storage.nosql.couchbase.CouchbaseUtils.fromMicroseconds;
import static org.jobrunr.storage.nosql.couchbase.CouchbaseUtils.getIdAsUUID;
import static org.jobrunr.storage.nosql.couchbase.CouchbaseUtils.toCouchbaseId;
import static org.jobrunr.storage.nosql.couchbase.CouchbaseUtils.toMicroSeconds;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

public class CouchbaseStorageProvider extends AbstractStorageProvider implements NoSqlStorageProvider {
    
    public static final String DEFAULT_BUCKET_NAME = "jobrunr";
    public static final String DEFAULT_SCOPE_NAME = "jobrunr";
    
    private final Cluster cluster;
    private final Scope scope;
    private final Collection jobCollection;
    private final Collection metadataCollection;
    private final Collection recurringJobCollection;
    private final Collection backgroundJobServerCollection;
    private final String collectionPrefix;
    private final String bucketName;
    private final String scopeName;
    private final DurabilityLevel durabilityLevel;

    private JobDocumentMapper jobDocumentMapper;
    private BackgroundJobServerStatusDocumentMapper backgroundJobServerStatusDocumentMapper;
    private MetadataDocumentMapper metadataDocumentMapper;
    
    public CouchbaseStorageProvider(String connectionString, String username, String password) {
        this(Cluster.connect(connectionString, ClusterOptions.clusterOptions(username, password)),
             DEFAULT_BUCKET_NAME,
             DEFAULT_SCOPE_NAME,
             null,
             StorageProviderUtils.DatabaseOptions.CREATE,
             rateLimit().at1Request().per(SECOND));
    }

    public CouchbaseStorageProvider(Cluster cluster, String bucketName, String scopeName, String collectionPrefix,
                                    StorageProviderUtils.DatabaseOptions databaseOptions, RateLimiter changeListenerNotificationRateLimit) {
        this(cluster, bucketName, scopeName, collectionPrefix, databaseOptions, changeListenerNotificationRateLimit, DurabilityLevel.MAJORITY);
    }

    public CouchbaseStorageProvider(Cluster cluster, String bucketName, String scopeName, String collectionPrefix,
                                    StorageProviderUtils.DatabaseOptions databaseOptions, RateLimiter changeListenerNotificationRateLimit,
                                    DurabilityLevel durabilityLevel) {
        super(changeListenerNotificationRateLimit);

        validateCouchbaseCluster(cluster);

        this.cluster = cluster;
        this.bucketName = bucketName;
        this.scopeName = scopeName;
        this.collectionPrefix = collectionPrefix;
        this.durabilityLevel = durabilityLevel;

        Bucket bucket = cluster.bucket(bucketName);
        bucket.waitUntilReady(Duration.ofSeconds(30));
        this.scope = bucket.scope(scopeName);

        jobCollection = scope.collection(elementPrefixer(collectionPrefix, Jobs.NAME));
        metadataCollection = scope.collection(elementPrefixer(collectionPrefix, Metadata.NAME));
        recurringJobCollection = scope.collection(elementPrefixer(collectionPrefix, RecurringJobs.NAME));
        backgroundJobServerCollection = scope.collection(elementPrefixer(collectionPrefix, BackgroundJobServers.NAME));

        setUpStorageProvider(databaseOptions);
    }
    
    private void validateCouchbaseCluster(Cluster cluster) {
        try {
            cluster.ping();
        } catch (Exception e) {
            throw new StorageException("Could not connect to the Couchbase cluster. Please check your Couchbase configuration.", e);
        }
    }
    
    @Override
    public void setJobMapper(JobMapper jobMapper) {
        this.jobDocumentMapper = new JobDocumentMapper(jobMapper);
        this.backgroundJobServerStatusDocumentMapper = new BackgroundJobServerStatusDocumentMapper();
        this.metadataDocumentMapper = new MetadataDocumentMapper();
    }
    
    @Override
    public void setUpStorageProvider(StorageProviderUtils.DatabaseOptions databaseOptions) {
        if (databaseOptions == StorageProviderUtils.DatabaseOptions.CREATE) {
            new CouchbaseCreator(cluster, bucketName, scopeName, collectionPrefix).runMigrations();
        } else if (databaseOptions == StorageProviderUtils.DatabaseOptions.SKIP_CREATE) {
            new CouchbaseCreator(cluster, bucketName, scopeName, collectionPrefix).validateCollections();
        }
    }
    
    @Override
    public void announceBackgroundJobServer(BackgroundJobServerStatus serverStatus) {
        try {
            backgroundJobServerCollection.insert(serverStatus.getId().toString(),
                                                 backgroundJobServerStatusDocumentMapper.toInsertDocument(serverStatus),
                                                 InsertOptions.insertOptions().durability(durabilityLevel));
        } catch (Exception e) {
            throw new StorageException("Unable to announce BackgroundJobServer.", e);
        }
    }
    
    @Override
    public boolean signalBackgroundJobServerAlive(BackgroundJobServerStatus serverStatus) {
        try {
            backgroundJobServerCollection.mutateIn(serverStatus.getId().toString(),
                                                   backgroundJobServerStatusDocumentMapper.toMutateInSpecs(serverStatus),
                                                   MutateInOptions.mutateInOptions().durability(durabilityLevel));
            GetResult currentDoc = backgroundJobServerCollection.get(serverStatus.getId().toString());
            return currentDoc.contentAsObject().getBoolean(BackgroundJobServers.FIELD_IS_RUNNING);
        } catch (DocumentNotFoundException e) {
            throw new ServerTimedOutException(serverStatus,
                                              new StorageException("BackgroundJobServer with id " + serverStatus.getId() + " was not found"));
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public void signalBackgroundJobServerStopped(BackgroundJobServerStatus serverStatus) {
        try {
            backgroundJobServerCollection.remove(serverStatus.getId().toString(), RemoveOptions.removeOptions().durability(durabilityLevel));
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public List<BackgroundJobServerStatus> getBackgroundJobServers() {
        try {
            String query = String.format("SELECT META(s).id FROM `%s` s ORDER BY s.%s ASC",
                                         elementPrefixer(collectionPrefix, BackgroundJobServers.NAME),
                                         BackgroundJobServers.FIELD_FIRST_HEARTBEAT);
            QueryResult result = scope.query(query, QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS));
            List<BackgroundJobServerStatus> servers = new ArrayList<>();
            for (JsonObject row : result.rowsAsObject()) {
                GetResult doc = backgroundJobServerCollection.get(row.getString("id"));
                servers.add(backgroundJobServerStatusDocumentMapper.toBackgroundJobServerStatus(doc.contentAsObject()));
            }
            return servers;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public UUID getLongestRunningBackgroundJobServerId() {
        try {
            String query = String.format("SELECT s.%s FROM `%s` s ORDER BY s.%s ASC LIMIT 1",
                                         toCouchbaseId(BackgroundJobServers.FIELD_ID),
                                         elementPrefixer(collectionPrefix, BackgroundJobServers.NAME),
                                         BackgroundJobServers.FIELD_FIRST_HEARTBEAT);
            QueryResult result = scope.query(query, QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS));
            if (result.rowsAsObject().isEmpty()) {
                return null;
            }
            return getIdAsUUID(result.rowsAsObject().get(0));
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public int removeTimedOutBackgroundJobServers(Instant heartbeatOlderThan) {
        try {
            long heartbeatThresholdMicros = toMicroSeconds(heartbeatOlderThan);
            String selectQuery = String.format("SELECT META(s).id FROM `%s` s WHERE s.%s < $heartbeatThreshold",
                                               elementPrefixer(collectionPrefix, BackgroundJobServers.NAME),
                                               BackgroundJobServers.FIELD_LAST_HEARTBEAT);
            JsonObject params = JsonObject.create().put("heartbeatThreshold", heartbeatThresholdMicros);
            QueryResult selectResult = scope.query(selectQuery,
                                                   QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS).parameters(params));
            
            List<String> idsToDelete = new ArrayList<>();
            for (JsonObject row : selectResult.rowsAsObject()) {
                idsToDelete.add(row.getString("id"));
            }
            
            if (idsToDelete.isEmpty()) {
                return 0;
            }
            
            cluster.transactions().run(ctx -> {
                for (String id : idsToDelete) {
                    ctx.remove(ctx.get(backgroundJobServerCollection, id));
                }
            });
            
            return idsToDelete.size();
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public void saveMetadata(JobRunrMetadata metadata) {
        try {
            metadataCollection.upsert(metadata.getId(),
                                      metadataDocumentMapper.toUpdateDocument(metadata),
                                      UpsertOptions.upsertOptions().durability(durabilityLevel));
            notifyMetadataChangeListeners();
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public List<JobRunrMetadata> getMetadata(String name) {
        try {
            String query = String.format("SELECT m.* FROM `%s` m WHERE m.%s = $name",
                                         elementPrefixer(collectionPrefix, Metadata.NAME),
                                         Metadata.FIELD_NAME);
            JsonObject params = JsonObject.create().put("name", name);
            QueryResult result = scope.query(query,
                                             QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS).parameters(params));
            List<JobRunrMetadata> metadataList = new ArrayList<>();
            for (JsonObject row : result.rowsAsObject()) {
                metadataList.add(metadataDocumentMapper.toJobRunrMetadata(row));
            }
            return metadataList;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public JobRunrMetadata getMetadata(String name, String owner) {
        try {
            GetResult result = metadataCollection.get(JobRunrMetadata.toId(name, owner));
            return metadataDocumentMapper.toJobRunrMetadata(result.contentAsObject());
        } catch (DocumentNotFoundException e) {
            return null;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public void deleteMetadata(String name) {
        try {
            String selectQuery = String.format("SELECT META(m).id FROM `%s` m WHERE m.%s = $name",
                                               elementPrefixer(collectionPrefix, Metadata.NAME),
                                               Metadata.FIELD_NAME);
            JsonObject params = JsonObject.create().put("name", name);
            QueryResult selectResult = scope.query(selectQuery,
                                                   QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS).parameters(params));
            
            List<String> idsToDelete = new ArrayList<>();
            for (JsonObject row : selectResult.rowsAsObject()) {
                idsToDelete.add(row.getString("id"));
            }
            
            if (!idsToDelete.isEmpty()) {
                cluster.transactions().run(ctx -> {
                    for (String id : idsToDelete) {
                        ctx.remove(ctx.get(metadataCollection, id));
                    }
                });
            }
            
            notifyMetadataChangeListeners(!idsToDelete.isEmpty());
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public void deleteMetadata(String name, String owner) {
        try {
            metadataCollection.remove(JobRunrMetadata.toId(name, owner), RemoveOptions.removeOptions().durability(durabilityLevel));
            notifyMetadataChangeListeners(true);
        } catch (DocumentNotFoundException e) {
            notifyMetadataChangeListeners(false);
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public Job save(Job job) throws ConcurrentJobModificationException {
        try (JobVersioner jobVersioner = new JobVersioner(job)) {
            if (jobVersioner.isNewJob()) {
                try {
                    jobCollection.insert(job.getId().toString(),
                                         jobDocumentMapper.toInsertDocument(job),
                                         InsertOptions.insertOptions().durability(durabilityLevel));
                } catch (com.couchbase.client.core.error.DocumentExistsException e) {
                    throw new ConcurrentJobModificationException(job);
                }
            } else {
                GetResult currentDoc;
                try {
                    currentDoc = jobCollection.get(job.getId().toString());
                } catch (DocumentNotFoundException e) {
                    throw new ConcurrentJobModificationException(job);
                }
                long currentVersion = currentDoc.contentAsObject().getLong(Jobs.FIELD_VERSION);
                if (currentVersion != job.getVersion() - 1) {
                    throw new ConcurrentJobModificationException(job);
                }
                try {
                    jobCollection.replace(job.getId().toString(),
                                          jobDocumentMapper.toUpdateDocument(job),
                                          ReplaceOptions.replaceOptions().cas(currentDoc.cas()).durability(durabilityLevel));
                } catch (com.couchbase.client.core.error.CasMismatchException e) {
                    throw new ConcurrentJobModificationException(job);
                }
            }
            jobVersioner.commitVersion();
        } catch (ConcurrentJobModificationException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException(e);
        }
        notifyJobStatsOnChangeListeners();
        return job;
    }
    
    @Override
    public List<Job> save(List<Job> jobs) throws ConcurrentJobModificationException {
        if (jobs.isEmpty()) {
            return jobs;
        }
        
        try (JobListVersioner jobListVersioner = new JobListVersioner(jobs)) {
            if (jobListVersioner.areNewJobs()) {
                cluster.transactions().run(ctx -> {
                    for (Job job : jobs) {
                        ctx.insert(jobCollection, job.getId().toString(), jobDocumentMapper.toInsertDocument(job));
                    }
                });
            } else {
                List<Job> concurrentModifiedJobs = new ArrayList<>();
                for (Job job : jobs) {
                    try {
                        GetResult currentDoc = jobCollection.get(job.getId().toString());
                        long currentVersion = currentDoc.contentAsObject().getLong(Jobs.FIELD_VERSION);
                        if (currentVersion != job.getVersion() - 1) {
                            concurrentModifiedJobs.add(job);
                        } else {
                            try {
                                jobCollection.replace(job.getId().toString(),
                                                      jobDocumentMapper.toUpdateDocument(job),
                                                      ReplaceOptions.replaceOptions().cas(currentDoc.cas()).durability(durabilityLevel));
                            } catch (com.couchbase.client.core.error.CasMismatchException e) {
                                concurrentModifiedJobs.add(job);
                            }
                        }
                    } catch (Exception e) {
                        throw new StorageException(e);
                    }
                }
                if (!concurrentModifiedJobs.isEmpty()) {
                    jobListVersioner.rollbackVersions(concurrentModifiedJobs);
                    throw new ConcurrentJobModificationException(concurrentModifiedJobs);
                }
            }
            jobListVersioner.commitVersions();
        } catch (ConcurrentJobModificationException e) {
            throw e;
        } catch (Exception e) {
            throw new StorageException(e);
        }
        notifyJobStatsOnChangeListenersIf(!jobs.isEmpty());
        return jobs;
    }
    
    @Override
    public Job getJobById(UUID id) throws JobNotFoundException {
        try {
            GetResult result = jobCollection.get(id.toString());
            return jobDocumentMapper.toJob(result.contentAsObject());
        } catch (DocumentNotFoundException e) {
            throw new JobNotFoundException(id);
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public long countJobs(StateName state) {
        try {
            String query = String.format("SELECT COUNT(*) as count FROM `%s` WHERE %s = $state",
                                         elementPrefixer(collectionPrefix, Jobs.NAME),
                                         Jobs.FIELD_STATE);
            JsonObject params = JsonObject.create().put("state", state.name());
            QueryResult result = scope.query(query,
                                             QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS).parameters(params));
            if (result.rowsAsObject().isEmpty()) {
                return 0;
            }
            return result.rowsAsObject().get(0).getLong("count");
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public List<Job> getJobList(StateName state, Instant updatedBefore, AmountRequest amountRequest) {
        try {
            long offset = amountRequest instanceof OffsetBasedPageRequest ? ((OffsetBasedPageRequest) amountRequest).getOffset() : 0;
            String query = String.format("SELECT %s FROM `%s` WHERE %s = $state AND %s < $updatedBefore ORDER BY %s LIMIT $limit OFFSET $offset",
                                         Jobs.FIELD_JOB_AS_JSON,
                                         elementPrefixer(collectionPrefix, Jobs.NAME),
                                         Jobs.FIELD_STATE,
                                         Jobs.FIELD_UPDATED_AT,
                                         toOrderByClause(amountRequest));
            JsonObject params = JsonObject.create()
                                          .put("state", state.name())
                                          .put("updatedBefore", toMicroSeconds(updatedBefore))
                                          .put("limit", amountRequest.getLimit())
                                          .put("offset", offset);
            QueryResult result = scope.query(query,
                                             QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS).parameters(params));
            List<Job> jobs = new ArrayList<>();
            for (JsonObject row : result.rowsAsObject()) {
                jobs.add(jobDocumentMapper.toJob(row));
            }
            return jobs;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public List<Job> getJobList(StateName state, AmountRequest amountRequest) {
        try {
            long offset = amountRequest instanceof OffsetBasedPageRequest ? ((OffsetBasedPageRequest) amountRequest).getOffset() : 0;
            String query = String.format("SELECT %s FROM `%s` WHERE %s = $state ORDER BY %s LIMIT $limit OFFSET $offset",
                                         Jobs.FIELD_JOB_AS_JSON,
                                         elementPrefixer(collectionPrefix, Jobs.NAME),
                                         Jobs.FIELD_STATE,
                                         toOrderByClause(amountRequest));
            JsonObject params = JsonObject.create().put("state", state.name()).put("limit", amountRequest.getLimit()).put("offset", offset);
            QueryResult result = scope.query(query,
                                             QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS).parameters(params));
            List<Job> jobs = new ArrayList<>();
            for (JsonObject row : result.rowsAsObject()) {
                jobs.add(jobDocumentMapper.toJob(row));
            }
            return jobs;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public List<Job> getCarbonAwareJobList(Instant deadlineBefore, AmountRequest amountRequest) {
        try {
            long offset = amountRequest instanceof OffsetBasedPageRequest ? ((OffsetBasedPageRequest) amountRequest).getOffset() : 0;
            String query = String.format(
                    "SELECT %s FROM `%s` WHERE %s = $state AND %s < $scheduledBefore ORDER BY %s LIMIT $limit OFFSET $offset",
                    Jobs.FIELD_JOB_AS_JSON,
                    elementPrefixer(collectionPrefix, Jobs.NAME),
                    Jobs.FIELD_STATE,
                    Jobs.FIELD_SCHEDULED_AT,
                    toOrderByClause(amountRequest));
            JsonObject params = JsonObject.create()
                                          .put("state", StateName.AWAITING.name())
                                          .put("scheduledBefore", toMicroSeconds(deadlineBefore))
                                          .put("limit", amountRequest.getLimit())
                                          .put("offset", offset);
            QueryResult result = scope.query(query,
                                             QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS).parameters(params));
            List<Job> jobs = new ArrayList<>();
            for (JsonObject row : result.rowsAsObject()) {
                jobs.add(jobDocumentMapper.toJob(row));
            }
            return jobs;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public List<Job> getScheduledJobs(Instant scheduledBefore, AmountRequest amountRequest) {
        try {
            long offset = amountRequest instanceof OffsetBasedPageRequest ? ((OffsetBasedPageRequest) amountRequest).getOffset() : 0;
            String query = String.format("SELECT %s FROM `%s` WHERE %s = $state AND %s < $scheduledBefore ORDER BY %s ASC LIMIT $limit OFFSET $offset",
                                         Jobs.FIELD_JOB_AS_JSON,
                                         elementPrefixer(collectionPrefix, Jobs.NAME),
                                         Jobs.FIELD_STATE,
                                         Jobs.FIELD_SCHEDULED_AT,
                                         Jobs.FIELD_SCHEDULED_AT);
            JsonObject params = JsonObject.create()
                                          .put("state", StateName.SCHEDULED.name())
                                          .put("scheduledBefore", toMicroSeconds(scheduledBefore))
                                          .put("limit", amountRequest.getLimit())
                                          .put("offset", offset);
            QueryResult result = scope.query(query,
                                             QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS).parameters(params));
            List<Job> jobs = new ArrayList<>();
            for (JsonObject row : result.rowsAsObject()) {
                jobs.add(jobDocumentMapper.toJob(row));
            }
            return jobs;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public int deletePermanently(UUID id) {
        try {
            jobCollection.remove(id.toString(), RemoveOptions.removeOptions().durability(durabilityLevel));
            notifyJobStatsOnChangeListenersIf(true);
            return 1;
        } catch (DocumentNotFoundException e) {
            return 0;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public int deleteJobsPermanently(StateName state, Instant updatedBefore) {
        try {
            long updatedBeforeMicros = toMicroSeconds(updatedBefore);
            String selectQuery = String.format("SELECT META(j).id FROM `%s` j WHERE j.%s = $state AND j.%s < $updatedBefore",
                                               elementPrefixer(collectionPrefix, Jobs.NAME),
                                               Jobs.FIELD_STATE,
                                               Jobs.FIELD_UPDATED_AT);
            JsonObject params = JsonObject.create().put("state", state.name()).put("updatedBefore", updatedBeforeMicros);
            QueryResult selectResult = scope.query(selectQuery,
                                                   QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS).parameters(params));
            
            List<String> idsToDelete = new ArrayList<>();
            for (JsonObject row : selectResult.rowsAsObject()) {
                idsToDelete.add(row.getString("id"));
            }
            
            if (idsToDelete.isEmpty()) {
                return 0;
            }
            
            cluster.transactions().run(ctx -> {
                for (String id : idsToDelete) {
                    ctx.remove(ctx.get(jobCollection, id));
                }
            });
            
            notifyJobStatsOnChangeListenersIf(true);
            return idsToDelete.size();
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public Set<String> getDistinctJobSignatures(StateName... states) {
        try {
            JsonArray stateArray = JsonArray.from(java.util.Arrays.stream(states).map(Enum::name).collect(Collectors.toList()));
            String query = String.format("SELECT DISTINCT j.%s FROM `%s` j WHERE j.%s IN $states",
                                         Jobs.FIELD_JOB_SIGNATURE,
                                         elementPrefixer(collectionPrefix, Jobs.NAME),
                                         Jobs.FIELD_STATE);
            JsonObject params = JsonObject.create().put("states", stateArray);
            QueryResult result = scope.query(query, QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS).parameters(params));
            Set<String> signatures = new HashSet<>();
            for (JsonObject row : result.rowsAsObject()) {
                signatures.add(row.getString(Jobs.FIELD_JOB_SIGNATURE));
            }
            return signatures;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public Instant getRecurringJobLatestScheduledInstant(String recurringJobId, StateName... states) {
        try {
            String query;
            JsonObject params = JsonObject.create().put("recurringJobId", recurringJobId);
            if (areAllStateNames(states)) {
                query = String.format("SELECT j.%s FROM `%s` j WHERE j.%s = $recurringJobId ORDER BY j.%s DESC LIMIT 1",
                                      Jobs.FIELD_SCHEDULED_AT,
                                      elementPrefixer(collectionPrefix, Jobs.NAME),
                                      Jobs.FIELD_RECURRING_JOB_ID,
                                      Jobs.FIELD_SCHEDULED_AT);
            } else {
                String stateList = java.util.Arrays.stream(states).map(Enum::name).map(s -> "'" + s + "'").collect(Collectors.joining(","));
                query = String.format("SELECT j.%s FROM `%s` j WHERE j.%s IN [%s] AND j.%s = $recurringJobId ORDER BY j.%s DESC LIMIT 1",
                                      Jobs.FIELD_SCHEDULED_AT,
                                      elementPrefixer(collectionPrefix, Jobs.NAME),
                                      Jobs.FIELD_STATE,
                                      stateList,
                                      Jobs.FIELD_RECURRING_JOB_ID,
                                      Jobs.FIELD_SCHEDULED_AT);
            }
            QueryResult result = scope.query(query,
                                             QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS).parameters(params));
            if (result.rowsAsObject().isEmpty()) {
                return null;
            }
            Long scheduledAt = result.rowsAsObject().get(0).getLong(Jobs.FIELD_SCHEDULED_AT);
            return fromMicroseconds(scheduledAt);
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public RecurringJob saveRecurringJob(RecurringJob recurringJob) {
        try {
            recurringJobCollection.upsert(recurringJob.getId(),
                                          jobDocumentMapper.toInsertDocument(recurringJob),
                                          UpsertOptions.upsertOptions().durability(durabilityLevel));
            return recurringJob;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public RecurringJobsResult getRecurringJobs() {
        try {
            String query = String.format("SELECT META(r).id FROM `%s` r ORDER BY r.%s",
                                         elementPrefixer(collectionPrefix, RecurringJobs.NAME),
                                         RecurringJobs.FIELD_CREATED_AT);
            QueryResult result = scope.query(query, QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS));
            List<RecurringJob> recurringJobs = new ArrayList<>();
            for (JsonObject row : result.rowsAsObject()) {
                GetResult doc = recurringJobCollection.get(row.getString("id"));
                recurringJobs.add(jobDocumentMapper.toRecurringJob(doc.contentAsObject()));
            }
            return new RecurringJobsResult(recurringJobs);
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public boolean recurringJobsUpdated(Long recurringJobsUpdatedHash) {
        try {
            String query = String.format("SELECT SUM(r.%s) as jobsHash FROM `%s` r",
                                         RecurringJobs.FIELD_CREATED_AT,
                                         elementPrefixer(collectionPrefix, RecurringJobs.NAME));
            QueryResult result = scope.query(query, QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS));
            if (result.rowsAsObject().isEmpty() || result.rowsAsObject().get(0).get("jobsHash") == null) {
                return !recurringJobsUpdatedHash.equals(0L);
            }
            Long hash = result.rowsAsObject().get(0).getLong("jobsHash");
            return !recurringJobsUpdatedHash.equals(hash);
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public int deleteRecurringJob(String id) {
        try {
            recurringJobCollection.remove(id, RemoveOptions.removeOptions().durability(durabilityLevel));
            return 1;
        } catch (DocumentNotFoundException e) {
            return 0;
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public JobStats getJobStats() {
        try {
            Instant instant = Instant.now();
            
            long allTimeSucceededCount = 0;
            try {
                GetResult metadataResult = metadataCollection.get(Metadata.STATS_ID);
                Number value = metadataResult.contentAsObject().getNumber(Metadata.FIELD_VALUE);
                allTimeSucceededCount = value != null ? value.longValue() : 0L;
            } catch (DocumentNotFoundException e) {
                // not created yet
            }
            
            String stateQuery = String.format("SELECT j.%s, COUNT(*) as count FROM `%s` j WHERE j.%s IS NOT NULL GROUP BY j.%s",
                                              Jobs.FIELD_STATE,
                                              elementPrefixer(collectionPrefix, Jobs.NAME),
                                              Jobs.FIELD_STATE,
                                              Jobs.FIELD_STATE);
            QueryResult stateResult = scope.query(stateQuery, QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS));
            Map<String, Long> stateCounts = new HashMap<>();
            for (JsonObject row : stateResult.rowsAsObject()) {
                stateCounts.put(row.getString(Jobs.FIELD_STATE), row.getLong("count"));
            }
            
            Long awaitingCount = stateCounts.getOrDefault(StateName.AWAITING.name(), 0L);
            Long scheduledCount = stateCounts.getOrDefault(StateName.SCHEDULED.name(), 0L);
            Long enqueuedCount = stateCounts.getOrDefault(StateName.ENQUEUED.name(), 0L);
            Long processingCount = stateCounts.getOrDefault(StateName.PROCESSING.name(), 0L);
            Long succeededCount = stateCounts.getOrDefault(StateName.SUCCEEDED.name(), 0L);
            Long failedCount = stateCounts.getOrDefault(StateName.FAILED.name(), 0L);
            Long deletedCount = stateCounts.getOrDefault(StateName.DELETED.name(), 0L);
            long total = scheduledCount + enqueuedCount + processingCount + succeededCount + failedCount;
            
            QueryResult recurringCountResult = scope.query(String.format("SELECT COUNT(*) as count FROM `%s`",
                                                                         elementPrefixer(collectionPrefix, RecurringJobs.NAME)),
                                                           QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS));
            int recurringCount = recurringCountResult.rowsAsObject().get(0).getLong("count").intValue();
            
            QueryResult serverCountResult = scope.query(String.format("SELECT COUNT(*) as count FROM `%s`",
                                                                      elementPrefixer(collectionPrefix, BackgroundJobServers.NAME)),
                                                        QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS));
            int serverCount = serverCountResult.rowsAsObject().get(0).getLong("count").intValue();
            
            return new JobStats(instant,
                                total,
                                awaitingCount,
                                scheduledCount,
                                enqueuedCount,
                                processingCount,
                                failedCount,
                                succeededCount,
                                allTimeSucceededCount,
                                deletedCount,
                                recurringCount,
                                serverCount);
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
    
    private String toOrderByClause(AmountRequest amountRequest) {
        List<OrderTerm> orderTerms = amountRequest.getAllOrderTerms(Job.ALLOWED_SORT_COLUMNS.keySet());
        if (orderTerms.isEmpty()) {
            return Jobs.FIELD_UPDATED_AT + " ASC";
        }
        return orderTerms.stream()
                .map(term -> term.getFieldName() + " " + term.getOrder().name())
                .collect(Collectors.joining(", "));
    }

    @Override
    public void publishTotalAmountOfSucceededJobs(int amount) {
        try {
            cluster.transactions().run(ctx -> {
                com.couchbase.client.java.transactions.TransactionGetResult txResult;
                JsonObject metadataDoc;
                try {
                    txResult = ctx.get(metadataCollection, Metadata.STATS_ID);
                    metadataDoc = txResult.contentAsObject();
                } catch (DocumentNotFoundException e) {
                    metadataDoc = JsonObject.create().put(Metadata.FIELD_VALUE, 0L);
                    txResult = null;
                }
                Number currentValue = metadataDoc.getNumber(Metadata.FIELD_VALUE);
                long newValue = (currentValue != null ? currentValue.longValue() : 0L) + amount;
                metadataDoc.put(Metadata.FIELD_VALUE, newValue);
                if (txResult != null) {
                    ctx.replace(txResult, metadataDoc);
                } else {
                    ctx.insert(metadataCollection, Metadata.STATS_ID, metadataDoc);
                }
            });
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }
}
