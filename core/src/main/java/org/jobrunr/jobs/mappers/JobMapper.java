package org.jobrunr.jobs.mappers;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.utils.mapper.JsonMapper;

public class JobMapper {

    private final JsonMapper jsonMapper;

    public JobMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public String serializeJob(Job job) {
        return jsonMapper.serialize(job);
    }

    public Job deserializeJob(String serializedJobAsString) {
        return jsonMapper.deserialize(serializedJobAsString, Job.class);
    }

    public String serializeRecurringJob(RecurringJob job) {
        return jsonMapper.serialize(job);
    }

    public RecurringJob deserializeRecurringJob(String serializedJobAsString) {
        return jsonMapper.deserialize(serializedJobAsString, RecurringJob.class);
    }

}
