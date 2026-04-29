package org.jobrunr.storage.nosql.couchbase.mapper;

import com.couchbase.client.java.json.JsonObject;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.SchedulableState;
import org.jobrunr.storage.StorageProviderUtils.Jobs;
import org.jobrunr.storage.StorageProviderUtils.RecurringJobs;

import static org.jobrunr.storage.nosql.couchbase.CouchbaseUtils.toCouchbaseId;
import static org.jobrunr.storage.nosql.couchbase.CouchbaseUtils.toMicroSeconds;


public class JobDocumentMapper {
    private final JobMapper jobMapper;

    public JobDocumentMapper(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    public JsonObject toInsertDocument(Job job) {
        final JsonObject document = JsonObject.create();
        document.put(toCouchbaseId(Jobs.FIELD_ID), job.getId().toString());
        document.put(Jobs.FIELD_VERSION, job.getVersion());
        document.put(Jobs.FIELD_JOB_AS_JSON, jobMapper.serializeJob(job));
        document.put(Jobs.FIELD_JOB_SIGNATURE, job.getJobSignature());
        document.put(Jobs.FIELD_STATE, job.getState().name());
        document.put(Jobs.FIELD_CREATED_AT, toMicroSeconds(job.getCreatedAt()));
        document.put(Jobs.FIELD_UPDATED_AT, toMicroSeconds(job.getUpdatedAt()));
        // Persist scheduledAt from current state if schedulable, otherwise preserve it from state history.
        // Unlike MongoDB ($set does partial updates), Couchbase replaces the full document on every write,
        // so we must carry scheduledAt forward across state transitions.
        if (job.getJobState() instanceof SchedulableState) {
            document.put(Jobs.FIELD_SCHEDULED_AT, toMicroSeconds(((SchedulableState) job.getJobState()).getScheduledAt()));
        } else {
            job.getJobStatesOfType(SchedulableState.class)
                    .reduce((a, b) -> b)
                    .ifPresent(state -> document.put(Jobs.FIELD_SCHEDULED_AT, toMicroSeconds(state.getScheduledAt())));
        }
        job.getRecurringJobId().ifPresent(recurringJobId -> document.put(Jobs.FIELD_RECURRING_JOB_ID, recurringJobId));
        return document;
    }

    public JsonObject toUpdateDocument(Job job) {
        return toInsertDocument(job);
    }

    public Job toJob(JsonObject document) {
        return jobMapper.deserializeJob(document.get(Jobs.FIELD_JOB_AS_JSON).toString());
    }

    public JsonObject toInsertDocument(RecurringJob recurringJob) {
        final JsonObject document = JsonObject.create();
        document.put(toCouchbaseId(RecurringJobs.FIELD_ID), recurringJob.getId());
        document.put(RecurringJobs.FIELD_VERSION, recurringJob.getVersion());
        document.put(RecurringJobs.FIELD_JOB_AS_JSON, jobMapper.serializeRecurringJob(recurringJob));
        document.put(RecurringJobs.FIELD_CREATED_AT, recurringJob.getCreatedAt().toEpochMilli());
        return document;
    }

    public RecurringJob toRecurringJob(JsonObject document) {
        return jobMapper.deserializeRecurringJob(document.get(RecurringJobs.FIELD_JOB_AS_JSON).toString());
    }
}
