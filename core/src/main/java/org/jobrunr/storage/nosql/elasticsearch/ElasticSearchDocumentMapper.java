package org.jobrunr.storage.nosql.elasticsearch;

import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProviderUtils.Jobs;
import org.jobrunr.storage.StorageProviderUtils.RecurringJobs;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers.*;
import static org.jobrunr.storage.StorageProviderUtils.Metadata;
import static org.jobrunr.utils.reflection.ReflectionUtils.autobox;

public class ElasticSearchDocumentMapper {

    private final JobMapper jobMapper;

    public ElasticSearchDocumentMapper(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    public Map<Object, Object> toXContentBuilderForInsert(BackgroundJobServerStatus serverStatus) {
        final Map<Object, Object> map = new LinkedHashMap<>();
        map.put(FIELD_ID, serverStatus.getId().toString());
        map.put(FIELD_WORKER_POOL_SIZE, serverStatus.getWorkerPoolSize());
        map.put(FIELD_POLL_INTERVAL_IN_SECONDS, serverStatus.getPollIntervalInSeconds());
        map.put(FIELD_DELETE_SUCCEEDED_JOBS_AFTER, serverStatus.getDeleteSucceededJobsAfter());
        map.put(FIELD_DELETE_DELETED_JOBS_AFTER, serverStatus.getPermanentlyDeleteDeletedJobsAfter());
        map.put(FIELD_FIRST_HEARTBEAT, serverStatus.getFirstHeartbeat());
        map.put(FIELD_LAST_HEARTBEAT, serverStatus.getLastHeartbeat());
        map.put(FIELD_IS_RUNNING, serverStatus.isRunning());
        map.put(FIELD_SYSTEM_TOTAL_MEMORY, serverStatus.getSystemTotalMemory());
        map.put(FIELD_SYSTEM_FREE_MEMORY, serverStatus.getSystemFreeMemory());
        map.put(FIELD_SYSTEM_CPU_LOAD, serverStatus.getSystemCpuLoad());
        map.put(FIELD_PROCESS_MAX_MEMORY, serverStatus.getProcessMaxMemory());
        map.put(FIELD_PROCESS_FREE_MEMORY, serverStatus.getProcessFreeMemory());
        map.put(FIELD_PROCESS_ALLOCATED_MEMORY, serverStatus.getProcessAllocatedMemory());
        map.put(FIELD_PROCESS_CPU_LOAD, serverStatus.getProcessCpuLoad());
        return map;
    }

    public Map<Object, Object> toXContentBuilderForUpdate(BackgroundJobServerStatus serverStatus) {
        final Map<Object, Object> map = new LinkedHashMap<>();
        map.put(FIELD_LAST_HEARTBEAT, serverStatus.getLastHeartbeat());
        map.put(FIELD_SYSTEM_FREE_MEMORY, serverStatus.getSystemFreeMemory());
        map.put(FIELD_SYSTEM_CPU_LOAD, serverStatus.getSystemCpuLoad());
        map.put(FIELD_PROCESS_FREE_MEMORY, serverStatus.getProcessFreeMemory());
        map.put(FIELD_PROCESS_ALLOCATED_MEMORY, serverStatus.getProcessAllocatedMemory());
        map.put(FIELD_PROCESS_CPU_LOAD, serverStatus.getProcessCpuLoad());
        return map;
    }

    public Map<Object, Object> toXContentBuilder(Job job) {
        final Map<Object, Object> map = new LinkedHashMap<>();
        map.put(Jobs.FIELD_JOB_AS_JSON, jobMapper.serializeJob(job));
        map.put(Jobs.FIELD_STATE, job.getState());
        map.put(Jobs.FIELD_UPDATED_AT, job.getUpdatedAt());
        map.put(Jobs.FIELD_JOB_SIGNATURE, job.getJobSignature());
        if (job.hasState(StateName.SCHEDULED)) {
            map.put(Jobs.FIELD_SCHEDULED_AT, job.getLastJobStateOfType(ScheduledState.class).map(ScheduledState::getScheduledAt).orElseThrow(IllegalStateException::new));
        }
        if(job.getRecurringJobId().isPresent()) {
            map.put(Jobs.FIELD_RECURRING_JOB_ID, job.getRecurringJobId().get());
        }
        return map;
    }

    public Map<Object, Object> toXContentBuilder(JobRunrMetadata metadata) {
        final Map<Object, Object> map = new LinkedHashMap<>();
        map.put(Metadata.FIELD_NAME, metadata.getName());
        map.put(Metadata.FIELD_OWNER, metadata.getOwner());
        map.put(Metadata.FIELD_VALUE, metadata.getValue());
        map.put(Metadata.FIELD_CREATED_AT, metadata.getCreatedAt());
        map.put(Metadata.FIELD_UPDATED_AT, metadata.getUpdatedAt());
        return map;
    }

    public Map<Object, Object> toXContentBuilder(RecurringJob job) {
        final Map<Object, Object> map = new LinkedHashMap<>();
        map.put(RecurringJobs.FIELD_JOB_AS_JSON, jobMapper.serializeRecurringJob(job));
        map.put(RecurringJobs.FIELD_CREATED_AT, job.getCreatedAt().toEpochMilli());
        return map;
    }

    public BackgroundJobServerStatus toBackgroundJobServerStatus(Map<Object, Object> map) {
        return new BackgroundJobServerStatus(
          autobox(map.get(FIELD_ID), UUID.class),
          autobox(map.get(FIELD_WORKER_POOL_SIZE), int.class),
          autobox(map.get(FIELD_POLL_INTERVAL_IN_SECONDS), int.class),
          autobox(map.get(FIELD_DELETE_SUCCEEDED_JOBS_AFTER), Duration.class),
          autobox(map.get(FIELD_DELETE_DELETED_JOBS_AFTER), Duration.class),
          autobox(map.get(FIELD_FIRST_HEARTBEAT), Instant.class),
          autobox(map.get(FIELD_LAST_HEARTBEAT), Instant.class),
          autobox(map.get(FIELD_IS_RUNNING), boolean.class),
          autobox(map.get(FIELD_SYSTEM_TOTAL_MEMORY), long.class),
          autobox(map.get(FIELD_SYSTEM_FREE_MEMORY), long.class),
          autobox(map.get(FIELD_SYSTEM_CPU_LOAD), double.class),
          autobox(map.get(FIELD_PROCESS_MAX_MEMORY), long.class),
          autobox(map.get(FIELD_PROCESS_FREE_MEMORY), long.class),
          autobox(map.get(FIELD_PROCESS_ALLOCATED_MEMORY), long.class),
          autobox(map.get(FIELD_PROCESS_CPU_LOAD), double.class)
        );
    }

    public JobRunrMetadata toMetadata(Hit<Map<String, Object>> searchHit) {
        return toMetadata(searchHit.source());
    }

    public JobRunrMetadata toMetadata(GetResponse<Map<String, Object>> response) {
        return toMetadata(response.source());
    }

    public JobRunrMetadata toMetadata(Map<String, Object> fieldMap) {
        if(fieldMap == null || fieldMap.isEmpty()) return null;

        return new JobRunrMetadata(
          autobox(fieldMap.get(Metadata.FIELD_NAME), String.class),
          autobox(fieldMap.get(Metadata.FIELD_OWNER), String.class),
          autobox(fieldMap.get(Metadata.FIELD_VALUE), String.class),
          autobox(fieldMap.get(Metadata.FIELD_CREATED_AT), Instant.class),
          autobox(fieldMap.get(Metadata.FIELD_UPDATED_AT), Instant.class));
    }


    public Job toJob(GetResponse response) {
        return jobMapper.deserializeJob(response.fields().get(Jobs.FIELD_JOB_AS_JSON).toString());
    }

    public Job toJob(Hit<?> hit) {
        String jobAsJson = hit.fields().get(Jobs.FIELD_JOB_AS_JSON).toString();
        return jobMapper.deserializeJob(jobAsJson);
    }

    public RecurringJob toRecurringJob(Hit<?> hit) {
        String jobAsJson = hit.fields().get(RecurringJobs.FIELD_JOB_AS_JSON).toString();
        return jobMapper.deserializeRecurringJob(jobAsJson);
    }
}
