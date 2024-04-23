package org.jobrunr.utils;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.classThatDoesNotExistJobDetails;

class JobUtilsTest {

    @Test
    void jobExists() {
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
}