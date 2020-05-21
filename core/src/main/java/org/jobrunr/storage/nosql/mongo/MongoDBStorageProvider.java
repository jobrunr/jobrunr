package org.jobrunr.storage.nosql.mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Facet;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.AbstractStorageProvider;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.Page;
import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.ServerTimedOutException;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.StorageProviderConstants;
import org.jobrunr.storage.StorageProviderConstants.BackgroundJobServers;
import org.jobrunr.storage.StorageProviderConstants.Jobs;
import org.jobrunr.storage.StorageProviderConstants.RecurringJobs;
import org.jobrunr.utils.resilience.RateLimiter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static com.mongodb.client.model.Aggregates.facet;
import static com.mongodb.client.model.Aggregates.limit;
import static com.mongodb.client.model.Aggregates.sortByCount;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lt;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Sorts.orderBy;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.utils.JobUtils.getJobSignature;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

public class MongoDBStorageProvider extends AbstractStorageProvider {

    private final MongoCollection<Document> jobCollection;
    private final MongoCollection<Document> recurringJobCollection;
    private final MongoCollection<Document> backgroundJobServerCollection;
    private final MongoCollection<Document> jobStatsCollection;

    private JobDocumentMapper jobDocumentMapper;
    private BackgroundJobServerStatusDocumentMapper backgroundJobServerStatusDocumentMapper;

    public MongoDBStorageProvider(String hostName, int port) {
        this(MongoClients.create(
                MongoClientSettings.builder()
                        .applyToClusterSettings(builder -> builder.hosts(asList(new ServerAddress(hostName, port))))
                        .build()));
    }

    public MongoDBStorageProvider(MongoClient mongoClient) {
        this(mongoClient.getDatabase("jobrunr"));
    }

    public MongoDBStorageProvider(MongoClient mongoClient, RateLimiter changeListenerNotificationRateLimit) {
        this(mongoClient.getDatabase("jobrunr"), changeListenerNotificationRateLimit);
    }

    public MongoDBStorageProvider(MongoDatabase mongoDatabase) {
        this(mongoDatabase, rateLimit().at2Requests().per(SECOND));
    }

    public MongoDBStorageProvider(MongoDatabase mongoDatabase, RateLimiter changeListenerNotificationRateLimit) {
        super(changeListenerNotificationRateLimit);
        jobCollection = mongoDatabase.getCollection(Jobs.NAME, Document.class);
        jobCollection.createIndex(Indexes.ascending(Jobs.FIELD_STATE));
        jobCollection.createIndex(Indexes.ascending(Jobs.FIELD_JOB_SIGNATURE));
        jobCollection.createIndex(Indexes.ascending(Jobs.FIELD_UPDATED_AT));
        jobCollection.createIndex(Indexes.descending(Jobs.FIELD_UPDATED_AT));

        recurringJobCollection = mongoDatabase.getCollection(RecurringJobs.NAME, Document.class);
        backgroundJobServerCollection = mongoDatabase.getCollection(BackgroundJobServers.NAME, Document.class);
        jobStatsCollection = mongoDatabase.getCollection(StorageProviderConstants.JobStats.NAME, Document.class);
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
        final UpdateResult updateResult = this.backgroundJobServerCollection.updateOne(eq(toMongoId(Jobs.FIELD_ID), serverStatus.getId()), backgroundJobServerStatusDocumentMapper.toUpdateDocument(serverStatus));
        if (updateResult.getModifiedCount() < 1) {
            throw new ServerTimedOutException(serverStatus, new StorageException("BackgroundJobServer with id " + serverStatus.getId() + " was not found"));
        }
        final Document document = this.backgroundJobServerCollection.find(eq(toMongoId(Jobs.FIELD_ID), serverStatus.getId())).projection(include(BackgroundJobServers.FIELD_IS_RUNNING)).first();
        return document.getBoolean(BackgroundJobServers.FIELD_IS_RUNNING);
    }

    @Override
    public List<BackgroundJobServerStatus> getBackgroundJobServers() {
        return this.backgroundJobServerCollection.find().map(backgroundJobServerStatusDocumentMapper::toBackgroundJobServerStatus).into(new ArrayList<>());
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
            final UpdateResult updateResult = jobCollection.updateOne(and(eq(toMongoId(Jobs.FIELD_ID), job.getId()), eq(Jobs.FIELD_VERSION, job.increaseVersion())), jobDocumentMapper.toUpdateDocument(job));
            if (updateResult.getModifiedCount() < 1) {
                throw new ConcurrentJobModificationException(job.getId());
            }
        }
        notifyOnChangeListeners();
        return job;
    }

    @Override
    public int delete(UUID id) {
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
            jobCollection.bulkWrite(jobsToUpdate);
        }
        notifyOnChangeListenersIf(jobs.size() > 0);
        return jobs;
    }

    @Override
    public List<Job> getJobs(StateName state, Instant updatedBefore, PageRequest pageRequest) {
        final Bson sortOrder = pageRequest.getOrder() == PageRequest.Order.ASC ? ascending(Jobs.FIELD_UPDATED_AT) : descending(Jobs.FIELD_UPDATED_AT);
        return jobCollection
                .find(and(eq(Jobs.FIELD_STATE, state.name()), lt(Jobs.FIELD_UPDATED_AT, toMicroSeconds(updatedBefore))))
                .sort(orderBy(sortOrder))
                .skip((int) pageRequest.getOffset())
                .limit(pageRequest.getLimit())
                .projection(include(Jobs.FIELD_JOB_AS_JSON))
                .map(jobDocumentMapper::toJob)
                .into(new ArrayList<>());
    }

    @Override
    public List<Job> getScheduledJobs(Instant scheduledBefore, PageRequest pageRequest) {
        final Bson sortOrder = pageRequest.getOrder() == PageRequest.Order.ASC ? ascending(Jobs.FIELD_UPDATED_AT) : descending(Jobs.FIELD_UPDATED_AT);
        return jobCollection
                .find(and(eq(Jobs.FIELD_STATE, SCHEDULED.name()), lt(Jobs.FIELD_SCHEDULED_AT, toMicroSeconds(scheduledBefore))))
                .sort(orderBy(sortOrder))
                .skip((int) pageRequest.getOffset())
                .limit(pageRequest.getLimit())
                .projection(include(Jobs.FIELD_JOB_AS_JSON))
                .map(jobDocumentMapper::toJob)
                .into(new ArrayList<>());
    }

    @Override
    public Long countJobs(StateName state) {
        return jobCollection.countDocuments(eq(Jobs.FIELD_STATE, state.name()));
    }

    @Override
    public List<Job> getJobs(StateName state, PageRequest pageRequest) {
        final Bson sortOrder = pageRequest.getOrder() == PageRequest.Order.ASC ? ascending(Jobs.FIELD_UPDATED_AT) : descending(Jobs.FIELD_UPDATED_AT);
        return jobCollection
                .find(eq(Jobs.FIELD_STATE, state.name()))
                .sort(orderBy(sortOrder))
                .skip((int) pageRequest.getOffset())
                .limit(pageRequest.getLimit())
                .projection(include(Jobs.FIELD_JOB_AS_JSON))
                .map(jobDocumentMapper::toJob)
                .into(new ArrayList<>());
    }

    @Override
    public Page<Job> getJobPage(StateName state, PageRequest pageRequest) {
        long count = countJobs(state);
        if (count > 0) {
            List<Job> jobs = getJobs(state, pageRequest);
            return new Page<>(count, jobs, pageRequest);
        }
        return new Page<>(0, new ArrayList<>(), pageRequest);
    }

    @Override
    public int deleteJobs(StateName state, Instant updatedBefore) {
        final DeleteResult deleteResult = jobCollection.deleteMany(and(eq(Jobs.FIELD_STATE, state.name()), lt(Jobs.FIELD_UPDATED_AT, toMicroSeconds(updatedBefore))));
        final long deletedCount = deleteResult.getDeletedCount();
        notifyOnChangeListenersIf(deletedCount > 0);
        return (int) deletedCount;
    }

    @Override
    public boolean exists(JobDetails jobDetails, StateName state) {
        return jobCollection.countDocuments(and(eq(Jobs.FIELD_STATE, state.name()), eq("jobSignature", getJobSignature(jobDetails)))) > 0;
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
        final Document jobStats = jobStatsCollection.find(eq(toMongoId(Jobs.FIELD_ID), StorageProviderConstants.JobStats.FIELD_STATS)).first();
        final List<Document> aggregates = jobCollection.aggregate(asList(facet(new Facet(Jobs.FIELD_STATE, sortByCount("$state"), limit(7))))).first().get(Jobs.FIELD_STATE, List.class);

        Long awaiting = getCount(AWAITING, jobStats, aggregates);
        Long scheduled = getCount(SCHEDULED, jobStats, aggregates);
        Long enqueued = getCount(ENQUEUED, jobStats, aggregates);
        Long processing = getCount(PROCESSING, jobStats, aggregates);
        Long failed = getCount(FAILED, jobStats, aggregates);
        Long succeeded = getCount(SUCCEEDED, jobStats, aggregates);

        final int recurringJobCount = (int) recurringJobCollection.countDocuments();
        final int backgroundJobServerCount = (int) backgroundJobServerCollection.countDocuments();

        return new JobStats(
                7L,
                awaiting,
                scheduled,
                enqueued,
                processing,
                failed,
                succeeded,
                recurringJobCount,
                backgroundJobServerCount
        );
    }

    @Override
    public void publishJobStatCounter(StateName state, int amount) {
        jobStatsCollection.updateOne(eq(toMongoId(StorageProviderConstants.JobStats.FIELD_ID), StorageProviderConstants.JobStats.FIELD_STATS), Updates.inc(state.name(), amount), new UpdateOptions().upsert(true));
    }

    private boolean notAllJobsAreNew(List<Job> jobs) {
        return jobs.stream().anyMatch(job -> job.getId() != null);
    }

    private boolean notAllJobsAreExisting(List<Job> jobs) {
        return jobs.stream().anyMatch(job -> job.getId() == null);
    }

    private boolean areNewJobs(List<Job> jobs) {
        return jobs.get(0).getId() == null;
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
}
