package org.jobrunr.storage.nosql.mongo;

import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class JobDocumentMapper {
    private final JobMapper jobMapper;

    public JobDocumentMapper(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    public Document toInsertDocument(Job job) {
        final Document document = new Document();
        document.put("_id", job.getId());
        document.put("version", job.getVersion());
        document.put("jobAsJson", jobMapper.serializeJob(job));
        document.put("jobSignature", job.getJobSignature());
        document.put("state", job.getState().name());
        document.put("createdAt", toMicroSeconds(job.getCreatedAt()));
        document.put("updatedAt", toMicroSeconds(job.getUpdatedAt()));
        if (job.hasState(StateName.SCHEDULED)) {
            document.put("scheduledAt", toMicroSeconds(((ScheduledState) job.getJobState()).getScheduledAt()));
        }
        return document;
    }

    public Document toUpdateDocument(Job job) {
        final Document document = new Document();
        document.put("version", job.getVersion());
        document.put("jobAsJson", jobMapper.serializeJob(job));
        document.put("state", job.getState().name());
        document.put("updatedAt", toMicroSeconds(job.getUpdatedAt()));
        if (job.hasState(StateName.SCHEDULED)) {
            document.put("scheduledAt", toMicroSeconds(((ScheduledState) job.getJobState()).getScheduledAt()));
        }
        return new Document("$set", document);
    }

    public UpdateOneModel<Document> toUpdateOneModel(Job job) {
        Document filterDocument = new Document();
        filterDocument.append("_id", job.getId());

        //Update doc
        Document updateDocument = toUpdateDocument(job);

        //Update option
        UpdateOptions updateOptions = new UpdateOptions();
        updateOptions.upsert(false); //if true, will create a new doc in case of unmatched find

        return new UpdateOneModel<>(filterDocument, updateDocument, updateOptions);
    }

    public Job toJob(Document document) {
        return jobMapper.deserializeJob(document.get("jobAsJson").toString());
    }

    public Document toInsertDocument(RecurringJob recurringJob) {
        final Document document = new Document();
        document.put("_id", recurringJob.getId());
        document.put("version", recurringJob.getVersion());
        document.put("jobAsJson", jobMapper.serializeRecurringJob(recurringJob));
        return document;
    }

    public Document toUpdateDocument(RecurringJob recurringJob) {
        final Document document = new Document();
        document.put("version", recurringJob.getVersion());
        document.put("jobAsJson", jobMapper.serializeRecurringJob(recurringJob));
        return document;
    }

    public RecurringJob toRecurringJob(Document document) {
        return jobMapper.deserializeRecurringJob(document.get("jobAsJson").toString());
    }

    private long toMicroSeconds(Instant instant) {
        return ChronoUnit.MICROS.between(Instant.EPOCH, instant);
    }
}
