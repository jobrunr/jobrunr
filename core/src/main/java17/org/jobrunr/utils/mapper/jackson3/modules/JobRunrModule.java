package org.jobrunr.utils.mapper.jackson3.modules;

import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.states.FailedState;
import tools.jackson.databind.module.SimpleModule;

public class JobRunrModule extends SimpleModule {

    public JobRunrModule() {
        addSerializer(JobParameter.class, new JobParameterSerializer());
        addDeserializer(JobParameter.class, new JobParameterDeserializer());
        addDeserializer(FailedState.class, new FailedStateDeserializer());
    }
}
