package org.jobrunr.jobs;

import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobDetails;

class JobDetailsTest {

    @Test
    void testJobDetails() {
        JobDetails jobDetails = jobDetails()
                .withClassName(TestService.class)
                .withMethodName("doWork")
                .withJobParameter(5)
                .build();

        assertThat(jobDetails.getClassName()).isEqualTo(TestService.class.getName());
        assertThat(jobDetails.getMethodName()).isEqualTo("doWork");
        assertThat(jobDetails.getStaticFieldName()).isEmpty();
        assertThat(jobDetails.getJobParameterTypes()).isEqualTo(new Class[]{Integer.class});
        assertThat(jobDetails.getJobParameterValues()).isEqualTo(new Object[]{5});
    }

    @Test
    void testJobDetailsWithoutParameters() {
        JobDetails jobDetails = jobDetails()
                .withClassName(TestService.class)
                .withMethodName("doWork")
                .build();

        assertThat(jobDetails.getClassName()).isEqualTo(TestService.class.getName());
        assertThat(jobDetails.getMethodName()).isEqualTo("doWork");
        assertThat(jobDetails.getStaticFieldName()).isEmpty();
        assertThat(jobDetails.getJobParameterTypes()).isEqualTo(new Class[]{});
        assertThat(jobDetails.getJobParameterValues()).isEqualTo(new Object[]{});
    }

}