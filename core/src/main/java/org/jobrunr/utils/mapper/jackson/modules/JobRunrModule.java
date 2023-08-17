package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.jobrunr.jobs.JobParameter;

public class JobRunrModule extends SimpleModule {

    public JobRunrModule() {
        this(new ObjectMapper());
    }

    public JobRunrModule(ObjectMapper objectMapper) {
        addSerializer(JobParameter.class, new JobParameterSerializer());
        addDeserializer(JobParameter.class, new JobParameterDeserializer(objectMapper));
    }
}
