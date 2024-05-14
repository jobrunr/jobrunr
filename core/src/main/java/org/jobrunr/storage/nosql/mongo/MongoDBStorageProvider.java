package org.jobrunr.storage.nosql.mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.MongoWriteException;
import com.mongodb.ServerAddress;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.UuidCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.jobrunr.jobs.Job;
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
import org.jobrunr.storage.navigation.AmountRequest;
import org.jobrunr.storage.navigation.OffsetBasedPageRequest;
import org.jobrunr.storage.nosql.NoSqlStorageProvider;
import org.jobrunr.storage.nosql.mongo.mapper.BackgroundJobServerStatusDocumentMapper;
import org.jobrunr.storage.nosql.mongo.mapper.JobDocumentMapper;
import org.jobrunr.storage.nosql.mongo.mapper.MetadataDocumentMapper;
import org.jobrunr.storage.nosql.mongo.mapper.MongoDBAmountRequestMapper;
import org.jobrunr.utils.resilience.RateLimiter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.limit;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Aggregates.sort;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Sorts.ascending;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.storage.JobRunrMetadata.toId;
import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;
import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.CREATE;
import static org.jobrunr.storage.StorageProviderUtils.Jobs;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_UPDATED_AT;
import static org.jobrunr.storage.StorageProviderUtils.Metadata;
import static org.jobrunr.storage.StorageProviderUtils.RecurringJobs;
import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.storage.nosql.mongo.MongoUtils.getIdAsUUID;
import static org.jobrunr.utils.TimeUtils.fromMicroseconds;
import static org.jobrunr.utils.TimeUtils.toMicroSeconds;
import static org.jobrunr.utils.reflection.ReflectionUtils.findMethod;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

public class MongoDBStorageProvider extends AbstractStorageProvider implements NoSqlStorageProvider {

    public static final String DEFAULT_DB_NAME = "jobrunr";

    private static final MongoDBAmountRequestMapper pageRequestMapper = new MongoDBAmountRequestMapper();

    private final String databaseName;
    private final MongoClient mongoClient;
    private final MongoDatabase jobrunrDatabase;
    private final MongoCollection<Document> jobCollection;
    private final MongoCollection<Document> recurringJobCollection;
    private final MongoCollection<Document> backgroundJobServerCollection;
    private final MongoCollection<Document> metadataCollection;
    private final String collectionPrefix;

    private JobDocumentMapper jobDocumentMapper;
    private BackgroundJobServerStatusDocumentMapper backgroundJobServerStatusDocumentMapper;
    private MetadataDocumentMapper metadataDocumentMapper;

    public MongoDBStorageProvider(String hostName, int port) {
        this(MongoClients.create(
                MongoClientSettings.builder()
                        .applyToClusterSettings(builder -> builder.hosts(singletonList(new ServerAddress(hostName, port))))
                        .codecRegistry(CodecRegistries.fromRegistries(
                                CodecRegistries.fromCodecs(new UuidCodec(UuidRepresentation.STANDARD)),
                                MongoClientSettings.getDefaultCodecRegistry()
                        ))
                        .build()));
    }

    public MongoDBStorageProvider(MongoClient mongoClient) {
        this(mongoClient, rateLimit().at1Request().per(SECOND));
    }

    public MongoDBStorageProvider(MongoClient mongoClient, String dbName) {
        this(mongoClient, dbName, null, CREATE, rateLimit().at1Request().per(SECOND));
    }

    public MongoDBStorageProvider(MongoClient mongoClient, String dbName, DatabaseOptions databaseOptions) {
        this(mongoClient, dbName, null, databaseOptions, rateLimit().at1Request().per(SECOND));
    }

    public MongoDBStorageProvider(MongoClient mongoClient, String dbName, String collectionPrefix) {
        this(mongoClient, dbName, collectionPrefix, CREATE, rateLimit().at1Request().per(SECOND));
    }

    public MongoDBStorageProvider(MongoClient mongoClient, String dbName, String collectionPrefix, DatabaseOptions databaseOptions) {
        this(mongoClient, dbName, collectionPrefix, databaseOptions, rateLimit().at1Request().per(SECOND));
    }

    public MongoDBStorageProvider(MongoClient mongoClient, RateLimiter changeListenerNotificationRateLimit) {
        this(mongoClient, null, null, CREATE, changeListenerNotificationRateLimit);
    }

    public MongoDBStorageProvider(MongoClient mongoClient, DatabaseOptions databaseOptions, RateLimiter changeListenerNotificationRateLimit) {
        this(mongoClient, null, null, databaseOptions, changeListenerNotificationRateLimit);
    }

    public MongoDBStorageProvider(MongoClient mongoClient, String dbName, String collectionPrefix, DatabaseOptions databaseOptions, RateLimiter changeListenerNotificationRateLimit) {
        super(changeListenerNotificationRateLimit);
        validateMongoClient(mongoClient);

        this.databaseName = ofNullable(dbName).orElse(DEFAULT_DB_NAME);
        this.collectionPrefix = collectionPrefix;
        this.mongoClient = mongoClient;

        setUpStorageProvider(databaseOptions);

        jobrunrDatabase = mongoClient.getDatabase(databaseName);
        jobCollection = jobrunrDatabase.getCollection(elementPrefixer(collectionPrefix, Jobs.NAME), Document.class);
        recurringJobCollection = jobrunrDatabase.getCollection(elementPrefixer(collectionPrefix, RecurringJobs.NAME), Document.class);
        backgroundJobServerCollection = jobrunrDatabase.getCollection(elementPrefixer(collectionPrefix, BackgroundJobServers.NAME), Document.class);
        metadataCollection = jobrunrDatabase.getCollection(elementPrefixer(collectionPrefix, Metadata.NAME), Document.class);
    }

    @Override
    public void setJobMapper(JobMapper jobMapper) {
        this.jobDocumentMapper = new JobDocumentMapper(jobMapper);
        this.backgroundJobServerStatusDocumentMapper = new BackgroundJobServerStatusDocumentMapper();
        this.metadataDocumentMapper = new MetadataDocumentMapper();
    }

    @Override
    public void setUpStorageProvider(DatabaseOptions databaseOptions) {
        if (CREATE == databaseOptions) {
            runMigrations(mongoClient, databaseName, collectionPrefix);
        } else {
            validateTables(mongoClient, databaseName, collectionPrefix);
        }
    }

    @Override
    public void announceBackgroundJobServer(BackgroundJobServerStatus serverStatus) {
        InsertOneResult result = this.backgroundJobServerCollection.insertOne(backgroundJobServerStatusDocumentMapper.toInsertDocument(serverStatus));
        if (!result.wasAcknowledged()) {
            throw new StorageException("Unable to announce BackgroundJobServer.");
        }
    }

    @Override
    public boolean signalBackgroundJobServerAlive(BackgroundJobServerStatus serverStatus) {
        final UpdateResult updateResult = this.backgroundJobServerCollection.updateOne(eq(toMongoId(BackgroundJobServers.FIELD_ID), serverStatus.getId()), backgroundJobServerStatusDocumentMapper.toUpdateDocument(serverStatus));
        if (updateResult.getModifiedCount() < 1) {
            throw new ServerTimedOutException(serverStatus, new StorageException("BackgroundJobServer with id " + serverStatus.getId() + " was not found"));
        }
        final Document document = this.backgroundJobServerCollection.find(eq(toMongoId(Jobs.FIELD_ID), serverStatus.getId())).projection(include(BackgroundJobServers.FIELD_IS_RUNNING)).first();
        return document != null && document.getBoolean(BackgroundJobServers.FIELD_IS_RUNNING);
    }

    @Override
    public void signalBackgroundJobServerStopped(BackgroundJobServerStatus serverStatus) {
        this.backgroundJobServerCollection.deleteOne(eq(toMongoId(BackgroundJobServers.FIELD_ID), serverStatus.getId()));
    }

    @Override
    public List<BackgroundJobServerStatus> getBackgroundJobServers() {
        return this.backgroundJobServerCollection
                .find()
                .sort(ascending(BackgroundJobServers.FIELD_FIRST_HEARTBEAT))
                .map(backgroundJobServerStatusDocumentMapper::toBackgroundJobServerStatus)
                .into(new ArrayList<>());
    }

    @Override
    public UUID getLongestRunningBackgroundJobServerId() {
        return this.backgroundJobServerCollection
                .find()
                .sort(ascending(BackgroundJobServers.FIELD_FIRST_HEARTBEAT))
                .projection(include(toMongoId(BackgroundJobServers.FIELD_ID)))
                .map(MongoUtils::getIdAsUUID)
                .first();
    }

    @Override
    public int removeTimedOutBackgroundJobServers(Instant heartbeatOlderThan) {
        final DeleteResult deleteResult = this.backgroundJobServerCollection.deleteMany(lt(BackgroundJobServers.FIELD_LAST_HEARTBEAT, heartbeatOlderThan));
        return (int) deleteResult.getDeletedCount();
    }

    @Override
    public void saveMetadata(JobRunrMetadata metadata) {
        this.metadataCollection.updateOne(eq(toMongoId(Metadata.FIELD_ID), metadata.getId()), metadataDocumentMapper.toUpdateDocument(metadata), new UpdateOptions().upsert(true));
        notifyMetadataChangeListeners();
    }

    @Override
    public List<JobRunrMetadata> getMetadata(String name) {
        return metadataCollection.find(eq(Metadata.FIELD_NAME, name))
                .map(metadataDocumentMapper::toJobRunrMetadata)
                .into(new ArrayList<>());
    }

    @Override
    public JobRunrMetadata getMetadata(String name, String owner) {
        Document document = metadataCollection.find(eq(toMongoId(Metadata.FIELD_ID), toId(name, owner))).first();
        return metadataDocumentMapper.toJobRunrMetadata(document);
    }

    @Override
    public void deleteMetadata(String name) {
        final DeleteResult deleteResult = metadataCollection.deleteMany(eq(Metadata.FIELD_NAME, name));
        long deletedCount = deleteResult.getDeletedCount();
        notifyMetadataChangeListeners(deletedCount > 0);
    }

    @Override
    public Job save(Job job) {
        try (JobVersioner jobVersioner = new JobVersioner(job)) {
            if (jobVersioner.isNewJob()) {
                jobCollection.insertOne(jobDocumentMapper.toInsertDocument(job));
            } else {
                final UpdateOneModel<Document> updateModel = jobDocumentMapper.toUpdateOneModel(job);
                final UpdateResult updateResult = jobCollection.updateOne(updateModel.getFilter(), updateModel.getUpdate());
                if (updateResult.getModifiedCount() < 1) {
                    throw new ConcurrentJobModificationException(job);
                }
            }
            jobVersioner.commitVersion();
        } catch (MongoWriteException e) {
            if (e.getError().getCode() == 11000) throw new ConcurrentJobModificationException(job, e);
            throw new StorageException(e);
        } catch (MongoException e) {
            throw new StorageException(e);
        }
        notifyJobStatsOnChangeListeners();
        return job;
    }

    @Override
    public int deletePermanently(UUID id) {
        final DeleteResult result = jobCollection.deleteOne(eq(toMongoId(Jobs.FIELD_ID), id));
        final int deletedCount = (int) result.getDeletedCount();
        notifyJobStatsOnChangeListenersIf(deletedCount > 0);
        return deletedCount;
    }

    @Override
    public Job getJobById(UUID id) {
        final Document document = jobCollection.find(eq(toMongoId(Jobs.FIELD_ID), id)).projection(include(Jobs.FIELD_JOB_AS_JSON)).first();
        if (document != null) {
            return jobDocumentMapper.toJob(document);
        }
        throw new JobNotFoundException(id);
    }

    @Override
    public long countJobs(StateName state) {
        return jobCollection.countDocuments(eq(Jobs.FIELD_STATE, state.name()));
    }

    @Override
    public List<Job> getJobList(StateName state, Instant updatedBefore, AmountRequest amountRequest) {
        return findJobs(and(eq(Jobs.FIELD_STATE, state.name()), lt(Jobs.FIELD_UPDATED_AT, toMicroSeconds(updatedBefore))), amountRequest);
    }

    @Override
    public List<Job> getCarbonAwareJobList(Instant deadline, AmountRequest amountRequest) {
        return findJobs(lt(Jobs.FIELD_CARBON_AWARE_DEADLINE, toMicroSeconds(deadline)), amountRequest);
    }

    @Override
    public List<Job> getJobList(StateName state, AmountRequest amountRequest) {
        return findJobs(eq(Jobs.FIELD_STATE, state.name()), amountRequest);
    }

    @Override
    public List<Job> getScheduledJobs(Instant scheduledBefore, AmountRequest amountRequest) {
        return findJobs(and(eq(Jobs.FIELD_STATE, SCHEDULED), lt(Jobs.FIELD_SCHEDULED_AT, toMicroSeconds(scheduledBefore))), amountRequest);
    }

    @Override
    public List<Job> save(List<Job> jobs) {
        if (jobs.isEmpty()) return jobs;

        try (JobListVersioner jobListVersioner = new JobListVersioner(jobs)) {
            if (jobListVersioner.areNewJobs()) {
                final List<Document> jobsToInsert = jobs.stream()
                        .map(job -> jobDocumentMapper.toInsertDocument(job))
                        .collect(toList());
                jobCollection.insertMany(jobsToInsert);
            } else {
                final List<WriteModel<Document>> jobsToUpdate = jobs.stream()
                        .map(job -> jobDocumentMapper.toUpdateOneModel(job))
                        .collect(toList());
                final BulkWriteResult bulkWriteResult = jobCollection.bulkWrite(jobsToUpdate);
                if (bulkWriteResult.getModifiedCount() != jobs.size()) {
                    //ugly workaround as we do not know which document did not update due to concurrent modification exception. So, we download them all and compare the lastUpdated
                    final Map<UUID, Long> mongoDbDocuments = new HashMap<>();
                    jobCollection
                            .find(in(toMongoId(Jobs.FIELD_ID), jobs.stream().map(Job::getId).collect(toList())))
                            .projection(include(Jobs.FIELD_ID, FIELD_UPDATED_AT))
                            .forEach(doc -> mongoDbDocuments.put(getIdAsUUID(doc), doc.getLong(FIELD_UPDATED_AT)));

                    final List<Job> concurrentModifiedJobs = jobs.stream()
                            .filter(job -> toMicroSeconds(job.getUpdatedAt()) != mongoDbDocuments.get(job.getId()))
                            .collect(toList());
                    jobListVersioner.rollbackVersions(concurrentModifiedJobs);
                    throw new ConcurrentJobModificationException(concurrentModifiedJobs);
                }
            }
            jobListVersioner.commitVersions();
        } catch (MongoException e) {
            throw new StorageException(e);
        }
        notifyJobStatsOnChangeListenersIf(!jobs.isEmpty());
        return jobs;
    }

    @Override
    public int deleteJobsPermanently(StateName state, Instant updatedBefore) {
        final DeleteResult deleteResult = jobCollection.deleteMany(and(eq(Jobs.FIELD_STATE, state.name()), lt(Jobs.FIELD_CREATED_AT, toMicroSeconds(updatedBefore))));
        final long deletedCount = deleteResult.getDeletedCount();
        notifyJobStatsOnChangeListenersIf(deletedCount > 0);
        return (int) deletedCount;
    }

    @Override
    public Set<String> getDistinctJobSignatures(StateName... states) {
        return jobCollection
                .distinct(Jobs.FIELD_JOB_SIGNATURE, in(Jobs.FIELD_STATE, stream(states).map(Enum::name).collect(toSet())), String.class)
                .into(new HashSet<>());
    }

    @Override
    public boolean recurringJobExists(String recurringJobId, StateName... states) {
        if (states.length < 1) {
            return jobCollection.countDocuments(eq(Jobs.FIELD_RECURRING_JOB_ID, recurringJobId)) > 0;
        }
        return jobCollection.countDocuments(and(in(Jobs.FIELD_STATE, stream(states).map(Enum::name).collect(toSet())), eq(Jobs.FIELD_RECURRING_JOB_ID, recurringJobId))) > 0;
    }

    @Override
    public long countRecurringJobInstances(String recurringJobId, StateName... states) {
        List<Bson> aggregationPipeline = new ArrayList<>();
        Bson matchStage = match(and(eq(Jobs.FIELD_RECURRING_JOB_ID, recurringJobId), in(Jobs.FIELD_STATE, stream(states).map(Enum::name).collect(toSet()))));
        aggregationPipeline.add(matchStage);
        Bson countStage = group(null, Accumulators.sum("count", 1));
        aggregationPipeline.add(countStage);
        Bson projectStage = project(fields(excludeId(), include("count")));
        aggregationPipeline.add(projectStage);
        AggregateIterable<Document> result = jobCollection.aggregate(aggregationPipeline);
        Document countDocument = result.first();
        if (countDocument != null) {
            return countDocument.getLong("count");
        } else {
            return 0L;
        }
    }


    @Override
    public RecurringJob saveRecurringJob(RecurringJob recurringJob) {
        recurringJobCollection.replaceOne(eq(toMongoId(RecurringJobs.FIELD_ID), recurringJob.getId()), jobDocumentMapper.toInsertDocument(recurringJob), new ReplaceOptions().upsert(true));
        return recurringJob;
    }

    @Override
    public RecurringJobsResult getRecurringJobs() {
        ArrayList<RecurringJob> recurringJobs = recurringJobCollection.find().map(jobDocumentMapper::toRecurringJob).into(new ArrayList<>());
        return new RecurringJobsResult(recurringJobs);
    }

    @Override
    public boolean recurringJobsUpdated(Long recurringJobsUpdatedHash) {
        AggregateIterable<Document> lastModifiedHash = recurringJobCollection.aggregate(asList(
                sort(ascending(RecurringJobs.FIELD_CREATED_AT)),
                group("$last_modified_hash", Accumulators.sum(RecurringJobs.FIELD_CREATED_AT, '$' + RecurringJobs.FIELD_CREATED_AT)),
                limit(1)));
        if (lastModifiedHash.first() != null) {
            Long value = lastModifiedHash.first().getLong(RecurringJobs.FIELD_CREATED_AT);
            return !recurringJobsUpdatedHash.equals(value);
        }
        return !recurringJobsUpdatedHash.equals(0L);
    }

    @Override
    public int deleteRecurringJob(String id) {
        final DeleteResult deleteResult = recurringJobCollection.deleteOne(eq(toMongoId(RecurringJobs.FIELD_ID), id));
        return (int) deleteResult.getDeletedCount();
    }

    @Override
    public JobStats getJobStats() {
        Instant instant = Instant.now();
        final Document succeededJobStats = metadataCollection.find(eq(toMongoId(Metadata.FIELD_ID), Metadata.STATS_ID)).first();
        final long allTimeSucceededCount = (succeededJobStats != null ? ((Number) succeededJobStats.get(Metadata.FIELD_VALUE)).longValue() : 0L);

        final List<Document> stateAggregation = jobCollection.aggregate(asList(
                        match(ne(Jobs.FIELD_STATE, null)),
                        project(fields(excludeId(), include(Jobs.FIELD_STATE))),
                        group("$state", Accumulators.sum(Jobs.FIELD_STATE, 1)),
                        limit(10)))
                .into(new ArrayList<>());


        Long awaitingCount = getCount(AWAITING, stateAggregation);
        Long scheduledCount = getCount(SCHEDULED, stateAggregation);
        Long enqueuedCount = getCount(ENQUEUED, stateAggregation);
        Long processingCount = getCount(PROCESSING, stateAggregation);
        Long succeededCount = getCount(SUCCEEDED, stateAggregation);
        Long failedCount = getCount(FAILED, stateAggregation);
        Long deletedCount = getCount(DELETED, stateAggregation);

        final long total = scheduledCount + enqueuedCount + processingCount + succeededCount + failedCount;
        final int recurringJobCount = (int) recurringJobCollection.countDocuments();
        final int backgroundJobServerCount = (int) backgroundJobServerCollection.countDocuments();

        return new JobStats(
                instant,
                total,
                awaitingCount,
                scheduledCount,
                enqueuedCount,
                processingCount,
                failedCount,
                succeededCount,
                allTimeSucceededCount,
                deletedCount,
                recurringJobCount,
                backgroundJobServerCount
        );
    }

    @Override
    public void publishTotalAmountOfSucceededJobs(int amount) {
        metadataCollection.updateOne(eq(toMongoId(Metadata.FIELD_ID), Metadata.STATS_ID), Updates.inc(Metadata.FIELD_VALUE, amount), new UpdateOptions().upsert(true));
    }

    @Override
    public Map<String, Optional<Instant>> loadRecurringJobsLatestScheduledRun() {
        Map<String, Optional<Instant>> lastRuns = new HashMap<>();

        List<RecurringJob> recurringJobs = getRecurringJobs();
        for (RecurringJob recJob : recurringJobs) {
            lastRuns.put(recJob.getId(), Optional.empty());
        }

        List<Bson> aggregationPipeline = Arrays.asList(
                Aggregates.group("$" + Jobs.FIELD_RECURRING_JOB_ID,
                        Accumulators.max("latestScheduledAt", "$" + Jobs.FIELD_SCHEDULED_AT)),
                Aggregates.project(Projections.fields(Projections.include("_id", "latestScheduledAt")))
        );

        AggregateIterable<Document> result = jobCollection.aggregate(aggregationPipeline);

        for (Document document : result) {
            String jobId = document.getString("_id");
            Long latestScheduledAtMicros = document.getLong("latestScheduledAt");
            Optional<Instant> latestScheduledAt = Optional.ofNullable(fromMicroseconds(latestScheduledAtMicros));
            lastRuns.put(jobId, latestScheduledAt);
        }

        return lastRuns;
    }

    private Long getCount(StateName stateName, List<Document> aggregates) {
        Predicate<Document> statePredicate = document -> stateName.name().equals(document.get(toMongoId(Jobs.FIELD_ID)));
        BiFunction<Optional<Document>, Integer, Integer> count = (document, defaultValue) -> document.map(doc -> doc.getInteger(Jobs.FIELD_STATE)).orElse(defaultValue);
        return (long) count.apply(aggregates.stream().filter(statePredicate).findFirst(), 0);
    }

    public static String toMongoId(String id) {
        return "_" + id;
    }

    private List<Job> findJobs(Bson query, AmountRequest amountRequest) {
        return jobCollection
                .find(query)
                .sort(pageRequestMapper.mapToSort(amountRequest))
                .skip((amountRequest instanceof OffsetBasedPageRequest) ? (int) ((OffsetBasedPageRequest) amountRequest).getOffset() : 0)
                .limit(amountRequest.getLimit())
                .projection(include(Jobs.FIELD_JOB_AS_JSON))
                .map(jobDocumentMapper::toJob)
                .into(new ArrayList<>());
    }

    private void validateMongoClient(MongoClient mongoClient) {
        Optional<Method> codecRegistryGetter = findMethod(mongoClient, "getCodecRegistry");
        if (codecRegistryGetter.isPresent()) {
            try {
                CodecRegistry codecRegistry = (CodecRegistry) codecRegistryGetter.get().invoke(mongoClient);
                UuidCodec uuidCodec = (UuidCodec) codecRegistry.get(UUID.class);
                if (UuidRepresentation.UNSPECIFIED == uuidCodec.getUuidRepresentation()) {
                    throw new StorageException("\n" +
                            "Since release 4.0.0 of the MongoDB Java Driver, the default BSON representation of java.util.UUID values has changed from JAVA_LEGACY to UNSPECIFIED.\n" +
                            "Applications that store or retrieve UUID values must explicitly specify which representation to use, via the uuidRepresentation property of MongoClientSettings.\n" +
                            "The good news is that JobRunr works both with the STANDARD as the JAVA_LEGACY uuidRepresentation. Please choose the one most appropriate for your application.");
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw shouldNotHappenException(e);
            }
        }
    }

    protected void runMigrations(MongoClient mongoClient, String dbName, String collectionPrefix) {
        new MongoDBCreator(mongoClient, dbName, collectionPrefix).runMigrations();
    }

    protected void validateTables(MongoClient mongoClient, String dbName, String collectionPrefix) {
        new MongoDBCreator(mongoClient, dbName, collectionPrefix).validateCollections();
    }

    // used to perform query analysis for performance tuning
    private void explainQuery(Bson query) {
        Document explainDocument = new Document();
        explainDocument.put("find", Jobs.NAME);
        explainDocument.put("filter", query);
        Document command = new Document();
        command.put("explain", explainDocument);
        final Document document = jobrunrDatabase.runCommand(command);
        System.out.println(document.toJson());
    }

    private void explainAggregation(List<Bson> query, String collectionName) {
        Document explainDocument = new Document();
        explainDocument.put("aggregate", collectionName);
        explainDocument.put("pipeline", query);
        explainDocument.put("cursor", new Document());
        Document command = new Document();
        command.put("explain", explainDocument);
        command.put("verbosity", "executionStats");
        final Document document = jobrunrDatabase.runCommand(command);
        System.out.println(document.toJson());
    }
}
