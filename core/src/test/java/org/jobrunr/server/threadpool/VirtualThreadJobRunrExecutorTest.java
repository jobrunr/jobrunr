package org.jobrunr.server.threadpool;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VirtualThreadJobRunrExecutorTest {

    @Test
    void testVirtualThreadJobRunrExecutor() {
        VirtualThreadJobRunrExecutor virtualThreadPoolJobRunrExecutor = new VirtualThreadJobRunrExecutor(8);
        assertThat(virtualThreadPoolJobRunrExecutor.executorService.getClass().getSimpleName()).isEqualTo("ThreadPerTaskExecutor");
    }
}