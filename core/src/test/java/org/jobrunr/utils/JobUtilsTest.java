package org.jobrunr.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

}