package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.jobrunr.jobs.JobParameter;

import java.time.Duration;
import java.time.Instant;

public class JobRunrTimeModule extends SimpleModule {

    public JobRunrTimeModule() {
        addSerializer(JobParameter.class, new JobParameterSerializer());
        addDeserializer(JobParameter.class, new JobParameterDeserializer());
        addSerializer(Instant.class, new InstantSerializer());
        addDeserializer(Instant.class, new InstantDeserializer());
        addSerializer(Duration.class, new DurationSerializer());
        addDeserializer(Duration.class, new DurationDeserializer());
    }
}
