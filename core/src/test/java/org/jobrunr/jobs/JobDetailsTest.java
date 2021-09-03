package org.jobrunr.jobs;

import org.jobrunr.stubs.TestJobRequest;
import org.jobrunr.stubs.TestJobRequest.TestJobRequestHandler;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.Test;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobDetails;

class JobDetailsTest {

    @Test
    void testJobDetails() {
        JobDetails jobDetails = jobDetails()
                .withClassName(TestService.class)
                .withMethodName("doWork")
                .withJobParameter(5)
                .build();

        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasStaticFieldName(null)
                .hasMethodName("doWork")
                .hasArgs(5);

        assertThat(jobDetails.getJobParameterTypes()).isEqualTo(new Class[]{Integer.class});
        assertThat(jobDetails.getJobParameterValues()).isEqualTo(new Object[]{5});
    }

    @Test
    void testJobDetailsWithoutParameters() {
        JobDetails jobDetails = jobDetails()
                .withClassName(TestService.class)
                .withMethodName("doWork")
                .build();

        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasStaticFieldName(null)
                .hasMethodName("doWork")
                .hasNoArgs();

        assertThat(jobDetails.getJobParameterTypes()).isEqualTo(new Class[]{});
        assertThat(jobDetails.getJobParameterValues()).isEqualTo(new Object[]{});
    }

    @Test
    void testJobDetailsFromJobRequest() {
        final TestJobRequest jobRequest = new TestJobRequest("some input");
        JobDetails jobDetails = new JobDetails(jobRequest);

        assertThat(jobDetails)
                .hasClass(TestJobRequestHandler.class)
                .hasStaticFieldName(null)
                .hasMethodName("run")
                .hasArgs(jobRequest)
                .isCacheable();
    }
}