package org.jobrunr.server.threadpool;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.jobrunr.server.BackgroundJobServer;
import org.mockito.internal.util.reflection.Whitebox;

import java.util.concurrent.ScheduledThreadPoolExecutor;

public class PlatformThreadPoolJobRunrExecutorAssert extends AbstractAssert<PlatformThreadPoolJobRunrExecutorAssert, PlatformThreadPoolJobRunrExecutor> {

    protected PlatformThreadPoolJobRunrExecutorAssert(PlatformThreadPoolJobRunrExecutor platformThreadPoolJobRunrExecutor) {
        super(platformThreadPoolJobRunrExecutor, PlatformThreadPoolJobRunrExecutorAssert.class);
    }

    public static PlatformThreadPoolJobRunrExecutorAssert assertThatZookeeperPoolOf(BackgroundJobServer backgroundJobServer) {
        return assertThat(Whitebox.getInternalState(backgroundJobServer, "zookeeperThreadPool"));
    }

    public static PlatformThreadPoolJobRunrExecutorAssert assertThat(PlatformThreadPoolJobRunrExecutor platformThreadPoolJobRunrExecutor) {
        return new PlatformThreadPoolJobRunrExecutorAssert(platformThreadPoolJobRunrExecutor);
    }

    public PlatformThreadPoolJobRunrExecutorAssert hasExecutorOfType(Class<ScheduledThreadPoolExecutor> scheduledThreadPoolExecutorClass) {
        Assertions.assertThat(actual.executorService.getClass()).isEqualTo(scheduledThreadPoolExecutorClass);
        return this;
    }

    public PlatformThreadPoolJobRunrExecutorAssert hasExecutorWithQueueSize(int amount) {
        Assertions.assertThat(actual.executorService.getQueue().size()).isEqualTo(amount);
        return this;
    }

    public PlatformThreadPoolJobRunrExecutorAssert hasExecutorWithCorePoolSize(int corePoolSize) {
        Assertions.assertThat(actual.executorService.getCorePoolSize()).isEqualTo(corePoolSize);
        return this;
    }
}
