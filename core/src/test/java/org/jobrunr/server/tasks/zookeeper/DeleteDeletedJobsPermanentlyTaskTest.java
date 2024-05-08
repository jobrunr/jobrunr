package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.tasks.AbstractTaskTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.within;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class DeleteDeletedJobsPermanentlyTaskTest extends AbstractTaskTest {

    DeleteDeletedJobsPermanentlyTask task;

    @BeforeEach
    void setUpTask() {
        task = new DeleteDeletedJobsPermanentlyTask(backgroundJobServer);
    }

    @Override
    protected void setUpBackgroundJobServerConfiguration(BackgroundJobServerConfiguration configuration) {
        configuration.andPermanentlyDeleteDeletedJobsAfter(Duration.ofDays(10));
    }

    @Test
    void testTask() {
        runTask(task);

        verify(storageProvider).deleteJobsPermanently(eq(DELETED), any());
    }

    @Test
    void testTaskTakesIntoAccountConfiguration() {
        runTask(task);

        verify(storageProvider).deleteJobsPermanently(eq(DELETED), assertArg(x -> assertThat(x).isCloseTo(now().minus(Duration.ofDays(10)), within(5, SECONDS))));
    }
}