package org.jobrunr.jobs;

import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobDetails;

class JobDetailsTest {

    @Test
    public void testJobDetails() {
        JobDetails jobDetails = jobDetails()
                .withLambdaType(JobLambda.class)
                .withClassName(TestService.class)
                .withMethodName("doWork")
                .withJobParameter(5)
                .build();

        assertThat(jobDetails.getLambdaType()).isEqualTo(JobLambda.class.getName());
        assertThat(jobDetails.getClassName()).isEqualTo(TestService.class.getName());
        assertThat(jobDetails.getMethodName()).isEqualTo("doWork");
        assertThat(jobDetails.getStaticFieldName()).isEmpty();
        assertThat(jobDetails.getJobParameterTypes()).isEqualTo(new Class[]{Integer.class});
        assertThat(jobDetails.getJobParameterValues()).isEqualTo(new Object[]{5});
    }

}