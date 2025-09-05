package org.jobrunr.micronaut.scheduling;

import jakarta.inject.Singleton;
import org.jobrunr.jobs.annotations.AsyncJob;
import org.jobrunr.jobs.annotations.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AsyncJob
public class AsyncJobTestServiceWithNestedJobService {

    private final AsyncJobTestService asyncJobTestService;
    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncJobTestServiceWithNestedJobService.class);

    public AsyncJobTestServiceWithNestedJobService(AsyncJobTestService asyncJobTestService) {
        this.asyncJobTestService = asyncJobTestService;
    }

    @Job(name = "my async job with nested jobs")
    public void testMethodThatCreatesOtherJobsAsAsyncJob() {
        LOGGER.info("Running AsyncJobTestServiceWithNestedJobService.testMethodThatCreatesOtherJobsAsAsyncJob in a job. It will create another job.");
        asyncJobTestService.testMethodAsAsyncJob();
    }

}
