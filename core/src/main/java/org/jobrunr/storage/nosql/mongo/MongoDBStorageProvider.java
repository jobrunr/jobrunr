package org.jobrunr.storage.nosql.mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.*;
import org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;
import org.jobrunr.storage.StorageProviderUtils.Jobs;
import org.jobrunr.storage.StorageProviderUtils.RecurringJobs;
import org.jobrunr.utils.resilience.RateLimiter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.mongodb.client.model.Aggregates.facet;
import static com.mongodb.client.model.Aggregates.limit;
import static com.mongodb.client.model.Aggregates.sortByCount;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Sorts.ascending;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.storage.StorageProviderUtils.JobStats.FIELD_ID;
import static org.jobrunr.storage.StorageProviderUtils.JobStats.FIELD_STATS;
import static org.jobrunr.storage.StorageProviderUtils.JobStats.NAME;
import static org.jobrunr.storage.StorageProviderUtils.areNewJobs;
import static org.jobrunr.storage.StorageProviderUtils.notAllJobsAreExisting;
import static org.jobrunr.storage.StorageProviderUtils.notAllJobsAreNew;
import static org.jobrunr.utils.JobUtils.getJobSignature;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

public class MongoDBStorageProvider extends AbstractStorageProvider {

    private static final MongoDBPageRequestMapper pageRequestMapper = new MongoDBPageRequestMapper();
    private final MongoCollection<Document> jobCollection;
    private final MongoCollection<Document> recurringJobCollection;
    private final MongoCollection<Document> backgroundJobServerCollection;
    private final MongoCollection<Document> jobStatsCollection;

    private JobDocumentMapper jobDocumentMapper;
    private BackgroundJobServerStatusDocumentMapper backgroundJobServerStatusDocumentMapper;
    private MongoDatabase jobrunrDatabase;

    public MongoDBStorageProvider(String hostName, int port) {
        this(MongoClients.create(
                MongoClientSettings.builder()
                        .applyToClusterSettings(builder -> builder.hosts(singletonList(new ServerAddress(hostName, port))))
                        .build()));
    }

    public MongoDBStorageProvider(MongoClient mongoClient) {
        this(mongoClient, rateLimit().at1Request().per(SECOND));
    }

    public MongoDBStorageProvider(MongoClient mongoClient, RateLimiter changeListenerNotificationRateLimit) {
        super(changeListenerNotificationRateLimit);

        if (jobRunrDatabaseExists(mongoClient)) {
            jobrunrDatabase = mongoClient.getDatabase("jobrunr");
            jobCollection = jobrunrDatabase.getCollection(Jobs.NAME, Document.class);
            recurringJobCollection = jobrunrDatabase.getCollection(RecurringJobs.NAME, Document.class);
            backgroundJobServerCollection = jobrunrDatabase.getCollection(BackgroundJobServers.NAME, Document.class);
            jobStatsCollection = jobrunrDatabase.getCollection(NAME, Document.class);
        } else {
            jobrunrDatabase = mongoClient.getDatabase("jobrunr");

            jobrunrDatabase.createCollection(Jobs.NAME);
            jobrunrDatabase.createCollection(RecurringJobs.NAME);
            jobrunrDatabase.createCollection(BackgroundJobServers.NAME);
            jobrunrDatabase.createCollection(NAME);

            jobCollection = jobrunrDatabase.getCollection(Jobs.NAME, Document.class);
            jobCollection.createIndex(Indexes.compoundIndex(Indexes.ascending(Jobs.FIELD_STATE), Indexes.ascending(Jobs.FIELD_SCHEDULED_AT)));
            jobCollection.createIndex(Indexes.compoundIndex(Indexes.ascending(Jobs.FIELD_STATE), Indexes.ascending(Jobs.FIELD_UPDATED_AT)));
            jobCollection.createIndex(Indexes.compoundIndex(Indexes.ascending(Jobs.FIELD_STATE), Indexes.descending(Jobs.FIELD_UPDATED_AT)));
            jobCollection.createIndex(Indexes.compoundIndex(Indexes.ascending(Jobs.FIELD_STATE), Indexes.ascending(Jobs.FIELD_CREATED_AT)));
            jobCollection.createIndex(Indexes.compoundIndex(Indexes.ascending(Jobs.FIELD_STATE), Indexes.ascending(Jobs.FIELD_JOB_SIGNATURE)));

            recurringJobCollection = jobrunrDatabase.getCollection(RecurringJobs.NAME, Document.class);
            backgroundJobServerCollection = jobrunrDatabase.getCollection(BackgroundJobServers.NAME, Document.class);
            jobStatsCollection = jobrunrDatabase.getCollection(NAME, Document.class);
        }
    }

    @Override
    public void setJobMapper(JobMapper jobMapper) {
        this.jobDocumentMapper = new JobDocumentMapper(jobMapper);
        this.backgroundJobServerStatusDocumentMapper = new BackgroundJobServerStatusDocumentMapper();
    }

    @Override
    public void announceBackgroundJobServer(BackgroundJobServerStatus serverStatus) {
        this.backgroundJobServerCollection.insertOne(backgroundJobServerStatusDocumentMapper.toInsertDocument(serverStatus));
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
    public int removeTimedOutBackgroundJobServers(Instant heartbeatOlderThan) {
        final DeleteResult deleteResult = this.backgroundJobServerCollection.deleteMany(lt(BackgroundJobServers.FIELD_LAST_HEARTBEAT, heartbeatOlderThan));
        return (int) deleteResult.getDeletedCount();
    }

    @Override
    public Job save(Job job) {
        if (job.getId() == null) {
            job.setId(UUID.randomUUID());
            jobCollection.insertOne(jobDocumentMapper.toInsertDocument(job));
        } else {
            final UpdateOneModel<Document> updateModel = jobDocumentMapper.toUpdateOneModel(job);
            final UpdateResult updateResult = jobCollection.updateOne(updateModel.getFilter(), updateModel.getUpdate());
            if (updateResult.getModifiedCount() < 1) {
                throw new ConcurrentJobModificationException(job);
            }
        }
        notifyJobStatsOnChangeListeners();
        return job;
    }

    @Override
    public int deletePermanently(UUID id) {
        final DeleteResult result = jobCollection.deleteOne(eq(toMongoId(Jobs.FIELD_ID), id));
        final int deletedCount = (int) result.getDeletedCount();
        notifyOnChangeListenersIf(deletedCount > 0);
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
    public List<Job> save(List<Job> jobs) {
        if (areNewJobs(jobs)) {
            if (notAllJobsAreNew(jobs)) {
                throw new IllegalArgumentException("All jobs must be either new (with id == null) or existing (with id != null)");
            }
            final List<Document> jobsToInsert = jobs.stream()
                    .peek(job -> job.setId(UUID.randomUUID()))
                    .map(job -> jobDocumentMapper.toInsertDocument(job))
                    .collect(toList());
            jobCollection.insertMany(jobsToInsert);
        } else {
            if (notAllJobsAreExisting(jobs)) {
                throw new IllegalArgumentException("All jobs must be either new (with id == null) or existing (with id != null)");
            }
            final List<WriteModel<Document>> jobsToUpdate = jobs.stream()
                    .map(job -> jobDocumentMapper.toUpdateOneModel(job))
                    .collect(toList());
            final BulkWriteResult bulkWriteResult = jobCollection.bulkWrite(jobsToUpdate);
            if (bulkWriteResult.getModifiedCount() != jobs.size()) {
                //ugly workaround as we do not know which document did not update due to concurrent modification exception. So, we download them all and compare the lastUpdated
                final Map<UUID, Job> mongoDbDocuments = new HashMap<>();
                jobCollection
                        .find(in(toMongoId(Jobs.FIELD_ID), jobs.stream().map(Job::getId).collect(toList())))
                        .projection(include(Jobs.FIELD_JOB_AS_JSON))
                        .map(jobDocumentMapper::toJob)
                        .forEach((Consumer<Job>) job -> mongoDbDocuments.put(job.getId(), job));

                final List<Job> concurrentModifiedJobs = jobs.stream()
                        .filter(job -> !job.getUpdatedAt().equals(mongoDbDocuments.get(job.getId()).getUpdatedAt()))
                        .collect(toList());
                throw new ConcurrentJobModificationException(concurrentModifiedJobs);

            }
        }
        notifyOnChangeListenersIf(!jobs.isEmpty());
        return jobs;
    }

    @Override
    public List<Job> getJobs(StateName state, Instant updatedBefore, PageRequest pageRequest) {
        return findJobs(and(eq(Jobs.FIELD_STATE, state.name()), lt(Jobs.FIELD_UPDATED_AT, toMicroSeconds(updatedBefore))), pageRequest);
    }

    @Override
    public List<Job> getScheduledJobs(Instant scheduledBefore, PageRequest pageRequest) {
        return findJobs(and(eq(Jobs.FIELD_STATE, SCHEDULED.name()), lt(Jobs.FIELD_SCHEDULED_AT, toMicroSeconds(scheduledBefore))), pageRequest);
    }

    @Override
    public Long countJobs(StateName state) {
        return jobCollection.countDocuments(eq(Jobs.FIELD_STATE, state.name()));
    }

    @Override
    public List<Job> getJobs(StateName state, PageRequest pageRequest) {
        return findJobs(eq(Jobs.FIELD_STATE, state.name()), pageRequest);
    }

    @Override
    public Page<Job> getJobPage(StateName state, PageRequest pageRequest) {
        return getJobPage(eq(Jobs.FIELD_STATE, state.name()), pageRequest);
    }

    @Override
    public int deleteJobs(StateName state, Instant updatedBefore) {
        final DeleteResult deleteResult = jobCollection.deleteMany(and(eq(Jobs.FIELD_STATE, state.name()), lt(Jobs.FIELD_CREATED_AT, toMicroSeconds(updatedBefore))));
        final long deletedCount = deleteResult.getDeletedCount();
        notifyOnChangeListenersIf(deletedCount > 0);
        return (int) deletedCount;
    }

    @Override
    public Set<String> getDistinctJobSignatures(StateName... states) {
        return jobCollection
                .distinct(Jobs.FIELD_JOB_SIGNATURE, in(Jobs.FIELD_STATE, stream(states).map(Enum::name).collect(toSet())), String.class)
                .into(new HashSet<>());
    }

    @Override
    public boolean exists(JobDetails jobDetails, StateName... states) {
        return jobCollection.countDocuments(and(in(Jobs.FIELD_STATE, stream(states).map(Enum::name).collect(toSet())), eq(Jobs.FIELD_JOB_SIGNATURE, getJobSignature(jobDetails)))) > 0;
    }

    @Override
    public boolean recurringJobExists(String recurringJobId, StateName... states) {
        return jobCollection.countDocuments(and(in(Jobs.FIELD_STATE, stream(states).map(Enum::name).collect(toSet())), eq(Jobs.FIELD_RECURRING_JOB_ID, recurringJobId))) > 0;
    }

    @Override
    public RecurringJob saveRecurringJob(RecurringJob recurringJob) {
        recurringJobCollection.replaceOne(eq(toMongoId(Jobs.FIELD_ID), recurringJob.getId()), jobDocumentMapper.toInsertDocument(recurringJob), new ReplaceOptions().upsert(true));
        return recurringJob;
    }

    @Override
    public List<RecurringJob> getRecurringJobs() {
        return recurringJobCollection.find().map(jobDocumentMapper::toRecurringJob).into(new ArrayList<>());
    }

    @Override
    public int deleteRecurringJob(String id) {
        final DeleteResult deleteResult = recurringJobCollection.deleteOne(eq(toMongoId(Jobs.FIELD_ID), id));
        return (int) deleteResult.getDeletedCount();
    }

    @Override
    public JobStats getJobStats() {
        Instant instant = Instant.now();
        final Document jobStats = jobStatsCollection.find(eq(toMongoId(Jobs.FIELD_ID), FIELD_STATS)).first();
        final List<Document> aggregates = jobCollection.aggregate(singletonList(facet(new Facet(Jobs.FIELD_STATE, sortByCount("$state"), limit(7))))).first().get(Jobs.FIELD_STATE, List.class);

        Long awaiting = getCount(AWAITING, jobStats, aggregates);
        Long scheduled = getCount(SCHEDULED, jobStats, aggregates);
        Long enqueued = getCount(ENQUEUED, jobStats, aggregates);
        Long processing = getCount(PROCESSING, jobStats, aggregates);
        Long failed = getCount(FAILED, jobStats, aggregates);
        Long succeeded = getCount(SUCCEEDED, jobStats, aggregates);
        Long deleted = getCount(DELETED, jobStats, aggregates);

        final int recurringJobCount = (int) recurringJobCollection.countDocuments();
        final int backgroundJobServerCount = (int) backgroundJobServerCollection.countDocuments();

        return new JobStats(
                instant,
                7L,
                awaiting,
                scheduled,
                enqueued,
                processing,
                failed,
                succeeded,
                deleted,
                recurringJobCount,
                backgroundJobServerCount
        );
    }

    @Override
    public void publishJobStatCounter(StateName state, int amount) {
        jobStatsCollection.updateOne(eq(toMongoId(FIELD_ID), FIELD_STATS), Updates.inc(state.name(), amount), new UpdateOptions().upsert(true));
    }

    private long toMicroSeconds(Instant instant) {
        return ChronoUnit.MICROS.between(Instant.EPOCH, instant);
    }

    private Long getCount(StateName stateName, Document jobStats, List<Document> aggregates) {
        Predicate<Document> statePredicate = document -> stateName.name().equals(document.get(toMongoId(Jobs.FIELD_ID)));
        BiFunction<Optional<Document>, Integer, Integer> count = (document, defaultValue) -> document.map(doc -> doc.getInteger("count")).orElse(defaultValue);

        long jobstatsCount = (jobStats != null ? (long) ofNullable(jobStats.getInteger(stateName.name())).orElse(0) : 0L);
        int aggregateCount = count.apply(aggregates.stream().filter(statePredicate).findFirst(), 0);
        return jobstatsCount + aggregateCount;
    }

    public static String toMongoId(String id) {
        return "_" + id;
    }


    private Page<Job> getJobPage(Bson query, PageRequest pageRequest) {
        long count = jobCollection.countDocuments(query);
        if (count > 0) {
            List<Job> jobs = findJobs(query, pageRequest);
            return new Page<>(count, jobs, pageRequest);
        }
        return new Page<>(0, new ArrayList<>(), pageRequest);
    }

    private List<Job> findJobs(Bson query, PageRequest pageRequest) {
        return jobCollection
                .find(query)
                .sort(pageRequestMapper.map(pageRequest))
                .skip((int) pageRequest.getOffset())
                .limit(pageRequest.getLimit())
                .projection(include(Jobs.FIELD_JOB_AS_JSON))
                .map(jobDocumentMapper::toJob)
                .into(new ArrayList<>());
    }

    private boolean jobRunrDatabaseExists(MongoClient mongoClient) {
        MongoIterable<String> allDatabases = mongoClient.listDatabaseNames();
        for (String dbName : allDatabases) {
            if ("jobrunr".equals(dbName)) return true;
        }
        return false;
    }

    // used to perform query analysis
    private void explainQuery(Bson query) {
        Document explainDocument = new Document();
        explainDocument.put("find", Jobs.NAME);
        explainDocument.put("filter", query);
        Document command = new Document();
        command.put("explain", explainDocument);
        final Document document = jobrunrDatabase.runCommand(command);
        System.out.println(document.toJson());
    }
}
