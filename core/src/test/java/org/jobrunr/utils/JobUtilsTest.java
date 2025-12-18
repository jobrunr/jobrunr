package org.jobrunr.utils;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.exceptions.JobParameterNotDeserializableException;
import org.jobrunr.scheduling.exceptions.JobClassNotFoundException;
import org.jobrunr.scheduling.exceptions.JobMethodNotFoundException;
import org.jobrunr.stubs.TestInvalidJobRequest;
import org.jobrunr.stubs.TestJobRequest;
import org.jobrunr.stubs.TestJobRequest.TestJobRequestHandler;
import org.jobrunr.stubs.TestService;
import org.jobrunr.stubs.TestServiceInterface;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.classThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobDetails;

class JobUtilsTest {

    @Test
    void assertJobExistsGivenJobDetails() {
        assertThatCode(() -> JobUtils.assertJobExists(jobDetails().withClassName(TestJobRequestHandler.class).withMethodName("run").withJobParameter(new TestJobRequest("input")).build()))
                .doesNotThrowAnyException();
        assertThatCode(() -> JobUtils.assertJobExists(jobDetails().withClassName(TestServiceInterface.class).withMethodName("doWork").build()))
                .doesNotThrowAnyException();
        assertThatCode(() -> JobUtils.assertJobExists(jobDetails().withClassName(TestService.class).withMethodName("doWork").build()))
                .doesNotThrowAnyException();
        assertThatCode(() -> JobUtils.assertJobExists(jobDetails().withClassName(TestService.class).withMethodName("doWork").withJobParameter(UUID.randomUUID()).build()))
                .doesNotThrowAnyException();
        assertThatCode(() -> JobUtils.assertJobExists(jobDetails().withClassName(TestService.class).withMethodName("doWork").withJobParameter(1).withJobParameter(2).build()))
                .doesNotThrowAnyException();
        assertThatCode(() -> JobUtils.assertJobExists(jobDetails().withClassName(TestService.class).withMethodName("doWorkThatTakesLong").withJobParameter(10L).build()))
                .doesNotThrowAnyException();
        assertThatCode(() -> JobUtils.assertJobExists(jobDetails().withClassName(TestService.class).withMethodName("getProcessedJobs").build()))
                .doesNotThrowAnyException();
        assertThatCode(() -> JobUtils.assertJobExists(jobDetails().withClassName(System.class).withStaticFieldName("out").withMethodName("println").withJobParameter("Hello, World!").build()))
                .doesNotThrowAnyException();

        assertThatCode(() -> JobUtils.assertJobExists(jobDetails().withClassName("org.jobrunr.stubs.TestServiceThatDoesNotExist").withMethodName("doWork").withJobParameter(1).withJobParameter(2).build()))
                .isInstanceOf(JobClassNotFoundException.class);
        assertThatCode(() -> JobUtils.assertJobExists(jobDetails().withClassName(TestJobRequestHandler.class).withMethodName("run").withJobParameter(new TestInvalidJobRequest()).build()))
                .isInstanceOf(JobMethodNotFoundException.class);
        assertThatCode(() -> JobUtils.assertJobExists(jobDetails().withClassName(TestService.class).withMethodName("methodThatDoesNotExist").withJobParameter(1).withJobParameter(2).build()))
                .isInstanceOf(JobMethodNotFoundException.class);
        assertThatCode(() -> JobUtils.assertJobExists(jobDetails().withClassName(TestService.class).withMethodName("doWork").withJobParameter(1).withJobParameter(new TestJobRequest("Hello, World!")).build())) // wrong parameter type
                .isInstanceOf(JobMethodNotFoundException.class);
        assertThatCode(() -> JobUtils.assertJobExists(jobDetails().withClassName(TestService.class).withMethodName("doWork").withJobParameter(1).withJobParameter(2).withJobParameter(3).build())) // too many parameters
                .isInstanceOf(JobMethodNotFoundException.class);

        var notDeserializableParameter = new JobParameter(Integer.class.getName(), Integer.class.getName(), 2, new JobParameterNotDeserializableException(Integer.class.getName(), new IllegalAccessException("Boom")));
        assertThatCode(() -> JobUtils.assertJobExists(jobDetails().withClassName(TestService.class).withMethodName("doWork").withJobParameter(notDeserializableParameter).build()))
                .isInstanceOf(JobMethodNotFoundException.class)
                .hasMessageContaining("JobParameterNotDeserializableException: one of the JobParameters of type");
        assertThatCode(() -> JobUtils.assertJobExists(jobDetails().withClassName(TestService.class).withMethodName("doWork").withJobParameter(1).withJobParameter(notDeserializableParameter).build()))
                .isInstanceOf(JobMethodNotFoundException.class)
                .hasMessageContaining("JobParameterNotDeserializableException: one of the JobParameters of type");
    }

    @Test
    void assertJobExistsGivenJobSignature() {
        assertThatCode(() -> JobUtils.assertJobExists("org.jobrunr.stubs.TestJobRequest$TestJobRequestHandler.run(org.jobrunr.stubs.TestJobRequest)")).doesNotThrowAnyException();
        assertThatCode(() -> JobUtils.assertJobExists("org.jobrunr.stubs.TestServiceInterface.doWork()")).doesNotThrowAnyException();
        assertThatCode(() -> JobUtils.assertJobExists("org.jobrunr.stubs.TestService.doWork()")).doesNotThrowAnyException();
        assertThatCode(() -> JobUtils.assertJobExists("org.jobrunr.stubs.TestService.doWork(java.util.UUID)")).doesNotThrowAnyException();
        assertThatCode(() -> JobUtils.assertJobExists("org.jobrunr.stubs.TestService.doWork(java.lang.Integer,java.lang.Integer)")).doesNotThrowAnyException();
        assertThatCode(() -> JobUtils.assertJobExists("org.jobrunr.stubs.TestService.doWorkThatTakesLong(java.lang.Integer)")).doesNotThrowAnyException();
        assertThatCode(() -> JobUtils.assertJobExists("org.jobrunr.stubs.TestService.getProcessedJobs()")).doesNotThrowAnyException();
        assertThatCode(() -> JobUtils.assertJobExists("java.lang.System.out.println(java.lang.String)")).doesNotThrowAnyException();
        assertThatCode(() -> JobUtils.assertJobExists("javax.sql.DataSource.getConnection()")).doesNotThrowAnyException();

        assertThatCode(() -> JobUtils.assertJobExists("org.jobrunr.stubs.TestServiceThatDoesNotExist.doWork(java.lang.Integer,java.lang.Integer)")).isInstanceOf(JobClassNotFoundException.class);
        assertThatCode(() -> JobUtils.assertJobExists("org.jobrunr.stubs.TestJobRequest$TestJobRequestHandler.run(org.jobrunr.stubs.TestInvalidJobRequest)")).isInstanceOf(JobMethodNotFoundException.class);
        assertThatCode(() -> JobUtils.assertJobExists("org.jobrunr.stubs.TestService.methodThatDoesNotExist(java.lang.Integer,java.lang.Integer)")).isInstanceOf(JobMethodNotFoundException.class);
        assertThatCode(() -> JobUtils.assertJobExists("org.jobrunr.stubs.TestService.doWork(java.lang.Integer,org.jobrunr.stubs.TestJobRequest)")).isInstanceOf(JobMethodNotFoundException.class); // wrong parameter type
        assertThatCode(() -> JobUtils.assertJobExists("org.jobrunr.stubs.TestService.doWork(java.lang.Integer,java.lang.Integer,java.lang.Integer,java.lang.Integer)")).isInstanceOf(JobMethodNotFoundException.class); // too many parameters
    }

    @Test
    void jobExists() {
        assertThat(JobUtils.jobExists("org.jobrunr.stubs.TestServiceInterface.doWork()")).isTrue();
        assertThat(JobUtils.jobExists("org.jobrunr.stubs.TestService.doWork(java.util.UUID)")).isTrue();
        assertThat(JobUtils.jobExists("org.jobrunr.stubs.TestService.getProcessedJobs()")).isTrue();
        assertThat(JobUtils.jobExists("org.jobrunr.stubs.TestService.doWorkThatTakesLong(java.lang.Integer)")).isTrue();
        assertThat(JobUtils.jobExists("org.jobrunr.stubs.TestService.doWork(java.lang.Integer,java.lang.Integer)")).isTrue();
        assertThat(JobUtils.jobExists("java.lang.System.out.println(java.lang.String)")).isTrue();
        assertThat(JobUtils.jobExists("javax.sql.DataSource.getConnection()")).isTrue();

        assertThat(JobUtils.jobExists("org.jobrunr.stubs.TestServiceThatDoesNotExist.doWork(java.lang.Integer,java.lang.Integer)")).isFalse();
        assertThat(JobUtils.jobExists("org.jobrunr.stubs.TestService.methodThatDoesNotExist(java.lang.Integer,java.lang.Integer)")).isFalse();
        assertThat(JobUtils.jobExists("org.jobrunr.stubs.TestService.doWork(java.util.UUID,org.jobrunr.stubs.JobParameterThatDoesNotExist)")).isFalse();
        assertThat(JobUtils.jobExists("org.jobrunr.stubs.TestService.doWork(java.lang.Integer,java.lang.Integer,java.lang.Integer,java.lang.Integer)")).isFalse(); // too many parameters
    }

    @Test
    void testGetJobAnnotation() {
        ListAppender<ILoggingEvent> logger = LoggerAssert.initFor(JobUtils.class);

        assertThatCode(() -> JobUtils.getJobAnnotation(classThatDoesNotExistJobDetails().build())).doesNotThrowAnyException();
        assertThat(logger).hasWarningMessageContaining("Trying to find Job Annotations for 'i.dont.exist.Class.notImportant(java.lang.Integer)' but the class could not be found. The Job name and other properties like retries and labels will not be set on the Job.");
    }

    @Test
    void testGetRecurringAnnotation() {
        ListAppender<ILoggingEvent> logger = LoggerAssert.initFor(JobUtils.class);

        assertThatCode(() -> JobUtils.getRecurringAnnotation(classThatDoesNotExistJobDetails().build())).doesNotThrowAnyException();
        assertThat(logger).hasWarningMessageContaining("Trying to find Job Annotations for 'i.dont.exist.Class.notImportant(java.lang.Integer)' but the class could not be found. The Job name and other properties like retries and labels will not be set on the Job.");
    }
}