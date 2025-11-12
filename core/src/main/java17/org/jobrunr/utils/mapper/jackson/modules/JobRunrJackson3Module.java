package org.jobrunr.utils.mapper.jackson.modules;

import org.jobrunr.jobs.JobParameter;
import tools.jackson.databind.module.SimpleModule;

public class JobRunrJackson3Module extends SimpleModule {

    public JobRunrJackson3Module() {
        addSerializer(JobParameter.class, new JobParameterSerializer());
        addDeserializer(JobParameter.class, new JobParameterDeserializer());
    }
}
