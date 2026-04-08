package org.jobrunr.server.threadpool;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformThreadPoolJobRunrExecutorTest {

    @Test
    void testPlatformThreadPoolJobRunrExecutor() {
        PlatformThreadPoolJobRunrExecutor platformThreadPoolJobRunrExecutor = new PlatformThreadPoolJobRunrExecutor(8);
        assertThat(platformThreadPoolJobRunrExecutor.executorService.getClass().getSimpleName()).isEqualTo("ScheduledThreadPoolExecutor");
    }

}