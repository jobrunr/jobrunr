package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.systemOutPrintLnJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

class DisplayNameFilterTest {

    private DisplayNameFilter displayNameFilter;

    @BeforeEach
    void setup() {
        displayNameFilter = new DisplayNameFilter();
    }

    @Test
    void testDisplayNameWithAnnotationUsingJobParametersAndMDCVariables() {
        MDC.put("customer.id", "1");
        Job job = anEnqueuedJob()
                .withJobDetails(jobDetails()
                        .withClassName(TestService.class)
                        .withMethodName("doWorkWithAnnotation")
                        .withJobParameter(5)
                        .withJobParameter("John Doe"))
                .build();

        displayNameFilter.onCreating(job);

        assertThat(job.getJobName()).isEqualTo("Doing some hard work for user John Doe (customerId: 1)");
    }

    @Test
    void testDisplayNameWithAnnotationUsingJobParametersAndMDCVariablesThatDoNotExist() {
        MDC.put("key-not-used-in-annotation", "1");
        Job job = anEnqueuedJob()
                .withJobDetails(jobDetails()
                        .withClassName(TestService.class)
                        .withMethodName("doWorkWithAnnotation")
                        .withJobParameter(5)
                        .withJobParameter("John Doe"))
                .build();

        displayNameFilter.onCreating(job);

        assertThat(job.getJobName()).isEqualTo("Doing some hard work for user John Doe (customerId: (customer.id is not found in MDC))");
    }

    @Test
    void testDisplayNameFromJobDetailsNormalMethod() {
        Job job = anEnqueuedJob()
                .withJobDetails(jobDetails()
                        .withClassName(TestService.class)
                        .withMethodName("doWork")
                        .withJobParameter(5.5))
                .build();

        displayNameFilter.onCreating(job);

        assertThat(job.getJobName()).isEqualTo("org.jobrunr.stubs.TestService.doWork(5.5)");
    }

    @Test
    void testDisplayNameFromJobDetailsStaticMethod() {
        Job job = anEnqueuedJob()
                .withJobDetails(systemOutPrintLnJobDetails("some message"))
                .build();

        displayNameFilter.onCreating(job);

        assertThat(job.getJobName()).isEqualTo("java.lang.System.out.println(some message)");
    }

    @Test
    void testDisplayNameFilterAlsoWorksWithJobContext() {
        Job job = anEnqueuedJob()
                .withJobDetails(jobDetails()
                        .withClassName(TestService.class)
                        .withMethodName("doWorkWithAnnotationAndJobContext")
                        .withJobParameter(5)
                        .withJobParameter("John Doe")
                        .withJobParameter(JobParameter.JobContext))
                .build();

        displayNameFilter.onCreating(job);

        assertThat(job.getJobName()).isEqualTo("Doing some hard work for user John Doe with id 5");
    }
}