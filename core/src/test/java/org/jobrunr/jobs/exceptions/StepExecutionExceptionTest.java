package org.jobrunr.jobs.exceptions;

import org.jobrunr.JobRunrException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StepExecutionExceptionTest {


    @Test
    void ifExceptionIsNormalExceptionThenDoNotRetryIsFalse() {
        StepExecutionException e = new StepExecutionException("test", new Exception());

        assertThat(e.isProblematicAndDoNotRetry()).isFalse();
    }

    @Test
    void ifExceptionIsJobRunrExceptionWithIsProblematicAndDoNotRetrySetToFalseThenDoNotRetryIsFalse() {
        StepExecutionException e = new StepExecutionException("test", new JobRunrException("an exception"));

        assertThat(e.isProblematicAndDoNotRetry()).isFalse();
    }

    @Test
    void ifExceptionIsJobRunrExceptionWithIsProblematicAndDoNotRetrySetToTrueThenDoNotRetryIsTrue() {
        StepExecutionException e = new StepExecutionException("test", new JobRunrException("an exception", true));

        assertThat(e.isProblematicAndDoNotRetry()).isTrue();
    }
}