package org.jobrunr.storage.nosql.elasticsearch;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.search.SearchHit;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;
import org.jobrunr.storage.StorageProviderUtils.Jobs;
import org.jobrunr.storage.StorageProviderUtils.RecurringJobs;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.jobrunr.utils.reflection.ReflectionUtils.autobox;

public class ElasticSearchDocumentMapper {

    private JobMapper jobMapper;

    public ElasticSearchDocumentMapper(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    public XContentBuilder toXContentBuilderForInsert(BackgroundJobServerStatus serverStatus) {
        try {
            XContentBuilder builder = JsonXContent.contentBuilder().prettyPrint();
            builder.startObject();
            builder.field(BackgroundJobServers.FIELD_ID, serverStatus.getId().toString());
            builder.field(BackgroundJobServers.FIELD_WORKER_POOL_SIZE, serverStatus.getWorkerPoolSize());
            builder.field(BackgroundJobServers.FIELD_POLL_INTERVAL_IN_SECONDS, serverStatus.getPollIntervalInSeconds());
            builder.field(BackgroundJobServers.FIELD_DELETE_SUCCEEDED_JOBS_AFTER, serverStatus.getDeleteSucceededJobsAfter());
            builder.field(BackgroundJobServers.FIELD_DELETE_DELETED_JOBS_AFTER, serverStatus.getPermanentlyDeleteDeletedJobsAfter());
            builder.field(BackgroundJobServers.FIELD_FIRST_HEARTBEAT, serverStatus.getFirstHeartbeat());
            builder.field(BackgroundJobServers.FIELD_LAST_HEARTBEAT, serverStatus.getLastHeartbeat());
            builder.field(BackgroundJobServers.FIELD_IS_RUNNING, serverStatus.isRunning());
            builder.field(BackgroundJobServers.FIELD_SYSTEM_TOTAL_MEMORY, serverStatus.getSystemTotalMemory());
            builder.field(BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY, serverStatus.getSystemFreeMemory());
            builder.field(BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD, serverStatus.getSystemCpuLoad());
            builder.field(BackgroundJobServers.FIELD_PROCESS_MAX_MEMORY, serverStatus.getProcessMaxMemory());
            builder.field(BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY, serverStatus.getProcessFreeMemory());
            builder.field(BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY, serverStatus.getProcessAllocatedMemory());
            builder.field(BackgroundJobServers.FIELD_PROCESS_CPU_LOAD, serverStatus.getProcessCpuLoad());
            builder.endObject();
            return builder;
        } catch (IOException e) {
            throw new RuntimeException("Should never happen", e);
        }
    }

    public XContentBuilder toXContentBuilderForUpdate(BackgroundJobServerStatus serverStatus) {
        try {
            XContentBuilder builder = JsonXContent.contentBuilder().prettyPrint();
            builder.startObject();
            builder.field(BackgroundJobServers.FIELD_LAST_HEARTBEAT, serverStatus.getLastHeartbeat());
            builder.field(BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY, serverStatus.getSystemFreeMemory());
            builder.field(BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD, serverStatus.getSystemCpuLoad());
            builder.field(BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY, serverStatus.getProcessFreeMemory());
            builder.field(BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY, serverStatus.getProcessAllocatedMemory());
            builder.field(BackgroundJobServers.FIELD_PROCESS_CPU_LOAD, serverStatus.getProcessCpuLoad());
            builder.endObject();
            return builder;
        } catch (IOException e) {
            throw new RuntimeException("Should never happen", e);
        }
    }

    public XContentBuilder toXContentBuilder(Job job) {
        try {
            XContentBuilder builder = JsonXContent.contentBuilder().prettyPrint();
            builder.startObject();
            builder.field(Jobs.FIELD_JOB_AS_JSON, jobMapper.serializeJob(job));
            builder.field(Jobs.FIELD_STATE, job.getState());
            builder.field(Jobs.FIELD_UPDATED_AT, job.getUpdatedAt());
            builder.field(Jobs.FIELD_JOB_SIGNATURE, job.getJobSignature());
            if (job.hasState(StateName.SCHEDULED)) {
                builder.field(Jobs.FIELD_SCHEDULED_AT, job.getLastJobStateOfType(ScheduledState.class).map(ScheduledState::getScheduledAt).orElseThrow(() -> new IllegalStateException()));
            }
            builder.field(Jobs.FIELD_RECURRING_JOB_ID, job.getJobStatesOfType(ScheduledState.class).findFirst().map(ScheduledState::getRecurringJobId).orElse(null));
            builder.endObject();
            return builder;
        } catch (IOException e) {
            throw new RuntimeException("Should never happen", e);
        }
    }

    public XContentBuilder toXContentBuilder(RecurringJob job) {
        try {
            XContentBuilder builder = JsonXContent.contentBuilder().prettyPrint();
            builder.startObject();
            builder.field(RecurringJobs.FIELD_JOB_AS_JSON, jobMapper.serializeRecurringJob(job));
            builder.endObject();
            return builder;
        } catch (IOException e) {
            throw new RuntimeException("Should never happen", e);
        }
    }

    public BackgroundJobServerStatus toBackgroundJobServerStatus(SearchHit searchHit) {
        Map<String, Object> fieldMap = searchHit.getSourceAsMap();
        return new BackgroundJobServerStatus(
                autobox(fieldMap.get(BackgroundJobServers.FIELD_ID), UUID.class),
                autobox(fieldMap.get(BackgroundJobServers.FIELD_WORKER_POOL_SIZE), int.class),
                autobox(fieldMap.get(BackgroundJobServers.FIELD_POLL_INTERVAL_IN_SECONDS), int.class),
                autobox(fieldMap.get(BackgroundJobServers.FIELD_DELETE_SUCCEEDED_JOBS_AFTER), Duration.class),
                autobox(fieldMap.get(BackgroundJobServers.FIELD_DELETE_DELETED_JOBS_AFTER), Duration.class),
                autobox(fieldMap.get(BackgroundJobServers.FIELD_FIRST_HEARTBEAT), Instant.class),
                autobox(fieldMap.get(BackgroundJobServers.FIELD_LAST_HEARTBEAT), Instant.class),
                autobox(fieldMap.get(BackgroundJobServers.FIELD_IS_RUNNING), boolean.class),
                autobox(fieldMap.get(BackgroundJobServers.FIELD_SYSTEM_TOTAL_MEMORY), long.class),
                autobox(fieldMap.get(BackgroundJobServers.FIELD_SYSTEM_FREE_MEMORY), long.class),
                autobox(fieldMap.get(BackgroundJobServers.FIELD_SYSTEM_CPU_LOAD), double.class),
                autobox(fieldMap.get(BackgroundJobServers.FIELD_PROCESS_MAX_MEMORY), long.class),
                autobox(fieldMap.get(BackgroundJobServers.FIELD_PROCESS_FREE_MEMORY), long.class),
                autobox(fieldMap.get(BackgroundJobServers.FIELD_PROCESS_ALLOCATED_MEMORY), long.class),
                autobox(fieldMap.get(BackgroundJobServers.FIELD_PROCESS_CPU_LOAD), double.class)
        );
    }

    public Job toJob(GetResponse response) {
        return jobMapper.deserializeJob(response.getField(Jobs.FIELD_JOB_AS_JSON).getValue());
    }

    public Job toJob(SearchHit hit) {
        String jobAsJson = hit.getFields().get(Jobs.FIELD_JOB_AS_JSON).getValue().toString();
        return jobMapper.deserializeJob(jobAsJson);
    }

    public RecurringJob toRecurringJob(SearchHit hit) {
        String jobAsJson = hit.getFields().get(RecurringJobs.FIELD_JOB_AS_JSON).getValue().toString();
        return jobMapper.deserializeRecurringJob(jobAsJson);
    }
}
