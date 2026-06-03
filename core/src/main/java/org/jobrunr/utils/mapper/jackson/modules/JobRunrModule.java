package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.server.costaware.CostAwareTotalSavings;

import static org.jobrunr.server.costaware.CostAwareTotalSavings.BackgroundJobServerSavings;

public class JobRunrModule extends SimpleModule {

    public JobRunrModule() {
        addSerializer(JobParameter.class, new JobParameterSerializer());
        addDeserializer(JobParameter.class, new JobParameterDeserializer());
        addDeserializer(BackgroundJobServerSavings.class, new BackgroundJobServerSavingsDeserializer());
        addDeserializer(CostAwareTotalSavings.class, new CostAwareTotalSavingsDeserializer());
    }
}
