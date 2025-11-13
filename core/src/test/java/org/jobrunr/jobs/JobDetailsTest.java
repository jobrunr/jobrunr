package org.jobrunr.jobs;

import org.jobrunr.jobs.exceptions.JobParameterNotDeserializableException;
import org.jobrunr.scheduling.exceptions.JobMethodNotFoundException;
import org.jobrunr.stubs.TestInvalidJobRequest;
import org.jobrunr.stubs.TestJobRequest;
import org.jobrunr.stubs.TestJobRequest.TestJobRequestHandler;
import org.jobrunr.stubs.TestService;
import org.jobrunr.utils.reflection.ReflectionTestClasses.GenericJobRequest;
import org.jobrunr.utils.reflection.ReflectionTestClasses.Level1JobRequest;
import org.jobrunr.utils.reflection.ReflectionTestClasses.Level2JobRequest;
import org.jobrunr.utils.reflection.ReflectionTestClasses.MyAsyncJobRequest;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobDetails;

class JobDetailsTest {

    @Test
    void testJobDetailsDefaultConstructor() {
        final JobDetails jobDetails = new JobDetails("some.class.Name", null, "run", emptyList());

        assertThat(jobDetails).isNotCacheable();
    }

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
                .hasArgs(5)
                .isNotCacheable();

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
                .hasNoArgs()
                .isNotCacheable();

        assertThat(jobDetails.getJobParameterTypes()).isEqualTo(new Class[]{});
        assertThat(jobDetails.getJobParameterValues()).isEqualTo(new Object[]{});
    }

    @Test
    void testGetJobParameterTypesWithNotDeserializableParameter() {
        JobDetails jobDetails = jobDetails()
                .withClassName(TestService.class)
                .withMethodName("doWork")
                .withJobParameter(new JobParameter(Integer.class.getName(), Integer.class.getName(), 2, new JobParameterNotDeserializableException(Integer.class.getName(), new IllegalAccessException("Boom"))))
                .build();

        assertThat(jobDetails)
                .hasClass(TestService.class)
                .hasStaticFieldName(null)
                .hasMethodName("doWork")
                .hasArgs(2)
                .hasNotDeserializableExceptionEqualTo(new JobParameterNotDeserializableException(Integer.class.getName(), IllegalAccessException.class.getName(), "Boom"))
                .isNotCacheable();

        assertThat(jobDetails.getJobParameterTypes()).isEqualTo(new Class[]{JobParameterNotDeserializableException.class});
        assertThat(jobDetails.getJobParameterValues()).isEqualTo(new Object[]{2});
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

    @Test
    void testJobDetailsFromInvalidJobRequest() {
        final TestInvalidJobRequest jobRequest = new TestInvalidJobRequest();
        assertThatThrownBy(() -> new JobDetails(jobRequest))
                .isInstanceOf(JobMethodNotFoundException.class);
    }

    @Test
    void testJobDetailsFromValidJobRequestUsingStaticField() {
        final GenericJobRequest jobRequest = new GenericJobRequest();
        assertThatCode(() -> new JobDetails(jobRequest)).doesNotThrowAnyException();
    }

    @Test
    void testJobDetailsFromValidJobRequestUsingGenerics() {
        final GenericJobRequest jobRequest = new GenericJobRequest();
        assertThatCode(() -> new JobDetails(jobRequest)).doesNotThrowAnyException();
    }

    @Test
    void testJobDetailsFromValidJobRequestUsingInheritance() {
        final Level1JobRequest<?, ?> jobRequest = new Level2JobRequest();
        assertThatCode(() -> new JobDetails(jobRequest)).doesNotThrowAnyException();
    }

    @Test
    void testJobDetailsFromValidJobRequestUsingMultipleLevelsOfInheritance() {
        final Level2JobRequest jobRequest = new Level2JobRequest();
        assertThatCode(() -> new JobDetails(jobRequest)).doesNotThrowAnyException();
    }

    @Test
    void testJobDetailsFromValidAsyncJobRequest() {
        final MyAsyncJobRequest jobRequest = new MyAsyncJobRequest();
        assertThatCode(() -> new JobDetails(jobRequest)).doesNotThrowAnyException();
    }
}