package org.jobrunr.tests.e2e.services;

import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.details.SerializedLambdaConverter;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.scheduling.BackgroundJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.SerializedLambda;

public class GeoService {
    Logger LOG = LoggerFactory.getLogger(GeoService.class);

    public void executeGeoTreeJob(JobContext jobContext, long geoNameId, UserId userId) {
        LOG.error("Running: " + geoNameId);
    }

    public void run() {
        LOG.error("Starting job");
        UserId userId = new UserId();
        userId.setValue("test");
        long geoNameId = 1234;

        JobLambda jobLambda = () -> executeGeoTreeJob(JobContext.Null, geoNameId, userId);

        SerializedLambda serializedLambda = SerializedLambdaConverter.toSerializedLambda(jobLambda);
        System.out.println("=======");
        System.out.println("serializedLambda " + serializedLambda.getImplMethodKind());
        System.out.println("=======");

        BackgroundJob.enqueue(() -> this.executeGeoTreeJob(JobContext.Null, geoNameId, userId));
    }
}
