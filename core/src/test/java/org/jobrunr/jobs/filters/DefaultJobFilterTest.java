package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.systemOutPrintLnJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

class DefaultJobFilterTest {

    private DefaultJobFilter defaultJobFilter;
    private TestService testService;

    @BeforeEach
    void setup() {
        defaultJobFilter = new DefaultJobFilter();
    }

    @Test
    void testDisplayNameByAnnotationReplacesVariables() {
        Job job = anEnqueuedJob().withoutName()
                .withJobDetails(() -> testService.doWorkWithAnnotationAndJobContext(67656, "the almighty user", JobContext.Null))
                .build();

        defaultJobFilter.onCreating(job);

        assertThat(job).hasJobName("Doing some hard work for user the almighty user with id 67656");
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
    void testLabelsIsUsedIfProvidedByJobBuilder() {
        Job job = anEnqueuedJob()
                .withLabels("TestLabel", "Email")
                .withJobDetails(jobDetails()
                        .withClassName(TestService.class)
                        .withMethodName("doWork")
                        .withJobParameter(2))
                .build();

        defaultJobFilter.onCreating(job);

        assertThat(job).hasLabels(List.of("TestLabel", "Email"));
    }

    @Test
    void testLabelsIsUsedIfProvidedByAnnotation() {
        Job job = anEnqueuedJob()
                .withJobDetails(() -> testService.doWorkWithJobAnnotationAndLabels(3, "customer name"))
                .build();

        defaultJobFilter.onCreating(job);

        assertThat(job).hasLabels(List.of("label-3 - customer name"));
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