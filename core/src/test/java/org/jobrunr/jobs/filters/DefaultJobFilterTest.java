package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.systemOutPrintLnJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

class DefaultJobFilterTest {

    private DefaultJobFilter defaultJobFilter;

    @BeforeEach
    void setup() {
        defaultJobFilter = new DefaultJobFilter();
    }

    @Test
    void testDisplayNameIsUsedIfProvidedByJobBuilder() {
        Job job = anEnqueuedJob()
                .withName("My job name")
                .withJobDetails(jobDetails()
                        .withClassName(TestService.class)
                        .withMethodName("doWork")
                        .withJobParameter(2))
                .build();

        defaultJobFilter.onCreating(job);

        assertThat(job).hasJobName("My job name");
    }

    @Test
    void testDisplayNameExceptionIsThrownIfJobBuilderIsUsedWithAnnotation() {
        Job job = anEnqueuedJob()
                .withName("My job name")
                .withJobDetails(jobDetails()
                        .withClassName(TestService.class)
                        .withMethodName("doWorkWithAnnotation")
                        .withJobParameter(5)
                        .withJobParameter("John Doe"))
                .build();

        assertThatThrownBy(() -> defaultJobFilter.onCreating(job))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You are combining the JobBuilder with the Job annotation. You can only use one of them.");
    }

    @Test
    void testAmountOfRetriesIsUsedIfProvidedByJobBuilder() {
        Job job = anEnqueuedJob()
                .withAmountOfRetries(3)
                .withJobDetails(jobDetails()
                        .withClassName(TestService.class)
                        .withMethodName("doWork")
                        .withJobParameter(2))
                .build();

        defaultJobFilter.onCreating(job);

        assertThat(job).hasAmountOfRetries(3);
    }

    @Test
    void testAmountOfRetriesExceptionIsThrownIfJobBuilderIsUsedWithAnnotation() {
        Job job = anEnqueuedJob()
                .withAmountOfRetries(3)
                .withJobDetails(jobDetails()
                        .withClassName(TestService.class)
                        .withMethodName("doWorkThatFails"))
                .build();

        assertThatThrownBy(() -> defaultJobFilter.onCreating(job))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You are combining the JobBuilder with the Job annotation. You can only use one of them.");
    }

    @Test
    void testDisplayNameWithAnnotationUsingJobParametersAndMDCVariables() {
        MDC.put("customer.id", "1");
        Job job = anEnqueuedJob()
                .withoutName()
                .withJobDetails(jobDetails()
                        .withClassName(TestService.class)
                        .withMethodName("doWorkWithAnnotation")
                        .withJobParameter(5)
                        .withJobParameter("John Doe"))
                .build();

        defaultJobFilter.onCreating(job);

        assertThat(job.getJobName()).isEqualTo("Doing some hard work for user John Doe (customerId: 1)");
    }

    @Test
    void testDisplayNameWithAnnotationUsingJobParametersAndMDCVariablesThatDoNotExist() {
        MDC.put("key-not-used-in-annotation", "1");
        Job job = anEnqueuedJob()
                .withoutName()
                .withJobDetails(jobDetails()
                        .withClassName(TestService.class)
                        .withMethodName("doWorkWithAnnotation")
                        .withJobParameter(5)
                        .withJobParameter("John Doe"))
                .build();

        defaultJobFilter.onCreating(job);

        assertThat(job.getJobName()).isEqualTo("Doing some hard work for user John Doe (customerId: (customer.id is not found in MDC))");
    }

    @Test
    void testDisplayNameFromJobDetailsNormalMethod() {
        Job job = anEnqueuedJob()
                .withoutName()
                .withJobDetails(jobDetails()
                        .withClassName(TestService.class)
                        .withMethodName("doWork")
                        .withJobParameter(5.5))
                .build();

        defaultJobFilter.onCreating(job);

        assertThat(job.getJobName()).isEqualTo("org.jobrunr.stubs.TestService.doWork(5.5)");
    }

    @Test
    void testDisplayNameFromJobDetailsStaticMethod() {
        Job job = anEnqueuedJob()
                .withoutName()
                .withJobDetails(systemOutPrintLnJobDetails("some message"))
                .build();

        defaultJobFilter.onCreating(job);

        assertThat(job.getJobName()).isEqualTo("java.lang.System.out.println(some message)");
    }

    @Test
    void testDisplayNameFilterAlsoWorksWithJobContext() {
        Job job = anEnqueuedJob()
                .withoutName()
                .withJobDetails(jobDetails()
                        .withClassName(TestService.class)
                        .withMethodName("doWorkWithAnnotationAndJobContext")
                        .withJobParameter(5)
                        .withJobParameter("John Doe")
                        .withJobParameter(JobParameter.JobContext))
                .build();

        defaultJobFilter.onCreating(job);

        assertThat(job.getJobName()).isEqualTo("Doing some hard work for user John Doe with id 5");
    }
}