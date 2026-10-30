package org.jobrunr.jobs.details;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.Test;

import static org.jobrunr.JobRunrAssertions.assertThat;

class JobDetailsGeneratorFactoryTest {

    @Test
    void createDefaultReturnsAWorkingJobDetailsGenerator() {
        JobDetailsGenerator jobDetailsGenerator = JobDetailsGeneratorFactory.createDefault();
        TestService testService = new TestService();

        JobDetails jobDetails = jobDetailsGenerator.toJobDetails((JobLambda) () -> testService.doWork(42));

        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasMethodName("doWork")
                .hasArgs(42);
    }

    @Test
    void cachingGeneratorDefaultsToTheSameBackendAsTheFactory() {
        assertThat(new CachingJobDetailsGenerator())
                .extracting("delegate")
                .isInstanceOf(JobDetailsGeneratorFactory.createDefault().getClass());
    }
}
