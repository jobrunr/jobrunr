package org.jobrunr.storage.nosql.mongo;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.StorageProviderUtils.Jobs;
import org.jobrunr.storage.StorageProviderUtils.RecurringJobs;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider.toMongoId;

public class JobDocumentMapper {
    private final JobMapper jobMapper;

    public JobDocumentMapper(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    public Document toInsertDocument(Job job) {
        final Document document = new Document();
        document.put(toMongoId(Jobs.FIELD_ID), job.getId());
        document.put(Jobs.FIELD_VERSION, job.getVersion());
        document.put(Jobs.FIELD_JOB_AS_JSON, jobMapper.serializeJob(job));
        document.put(Jobs.FIELD_JOB_SIGNATURE, job.getJobSignature());
        document.put(Jobs.FIELD_STATE, job.getState().name());
        document.put(Jobs.FIELD_CREATED_AT, toMicroSeconds(job.getCreatedAt()));
        document.put(Jobs.FIELD_UPDATED_AT, toMicroSeconds(job.getUpdatedAt()));
        if (job.hasState(StateName.SCHEDULED)) {
            document.put(Jobs.FIELD_SCHEDULED_AT, toMicroSeconds(job.<ScheduledState>getJobState().getScheduledAt()));
            document.put(Jobs.FIELD_RECURRING_JOB_ID, job.<ScheduledState>getJobState().getRecurringJobId());
        }
        return document;
    }

    public Document toUpdateDocument(Job job) {
        final Document document = new Document();
        document.put(Jobs.FIELD_VERSION, job.getVersion());
        document.put(Jobs.FIELD_JOB_AS_JSON, jobMapper.serializeJob(job));
        document.put(Jobs.FIELD_STATE, job.getState().name());
        document.put(Jobs.FIELD_UPDATED_AT, toMicroSeconds(job.getUpdatedAt()));
        if (job.hasState(StateName.SCHEDULED)) {
            document.put(Jobs.FIELD_SCHEDULED_AT, toMicroSeconds(((ScheduledState) job.getJobState()).getScheduledAt()));
            document.put(Jobs.FIELD_RECURRING_JOB_ID, job.<ScheduledState>getJobState().getRecurringJobId());
        }
        return new Document("$set", document);
    }

    public UpdateOneModel<Document> toUpdateOneModel(Job job) {
        Document filterDocument = new Document();
        filterDocument.append(toMongoId(Jobs.FIELD_ID), job.getId());
        filterDocument.append(Jobs.FIELD_VERSION, job.increaseVersion());

        //Update doc
        Document updateDocument = toUpdateDocument(job);

        //Update option
        UpdateOptions updateOptions = new UpdateOptions();
        updateOptions.upsert(false); //if true, will create a new doc in case of unmatched find

        return new UpdateOneModel<>(filterDocument, updateDocument, updateOptions);
    }

    public Job toJob(Document document) {
        return jobMapper.deserializeJob(document.get(Jobs.FIELD_JOB_AS_JSON).toString());
    }

    public Document toInsertDocument(RecurringJob recurringJob) {
        final Document document = new Document();
        document.put(toMongoId(RecurringJobs.FIELD_ID), recurringJob.getId());
        document.put(RecurringJobs.FIELD_VERSION, recurringJob.getVersion());
        document.put(RecurringJobs.FIELD_JOB_AS_JSON, jobMapper.serializeRecurringJob(recurringJob));
        return document;
    }

    public Bson byId(List<UUID> ids) {
        return Filters.in(toMongoId(Jobs.FIELD_ID), ids);
    }

    public RecurringJob toRecurringJob(Document document) {
        return jobMapper.deserializeRecurringJob(document.get(RecurringJobs.FIELD_JOB_AS_JSON).toString());
    }

    private long toMicroSeconds(Instant instant) {
        return ChronoUnit.MICROS.between(Instant.EPOCH, instant);
    }
}
