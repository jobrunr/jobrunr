package org.jobrunr.jobs.mappers;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.utils.mapper.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobMapper.class);

    private final JsonMapper jsonMapper;

    public JobMapper(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public String serializeJob(Job job) {
        String json = jsonMapper.serialize(job);
        if (json.length() > 5_000_000) {
            LOGGER.warn("Serialized Job(id='{}') JSON is very large, it exceeds 5 MB. Keep job arguments small to avoid performance issues.", job.getId());
        }
        return json;
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
