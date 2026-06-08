package org.jobrunr.utils.mapper.jackson3.modules;

import org.jobrunr.jobs.JobParameter;
import org.jobrunr.server.costaware.CostAwareTotalSavings;
import org.jobrunr.server.costaware.CostAwareTotalSavings.BackgroundJobServerSavings;
import tools.jackson.databind.module.SimpleModule;

public class JobRunrModule extends SimpleModule {

    public JobRunrModule() {
        addSerializer(JobParameter.class, new JobParameterSerializer());
        addDeserializer(JobParameter.class, new JobParameterDeserializer());
        addDeserializer(BackgroundJobServerSavings.class, new BackgroundJobServerSavingsDeserializer());
        addDeserializer(CostAwareTotalSavings.class, new CostAwareTotalSavingsDeserializer());
    }
}
