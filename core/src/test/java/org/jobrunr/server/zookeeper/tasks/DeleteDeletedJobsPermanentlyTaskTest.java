package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static java.time.Duration.ofHours;
import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class DeleteDeletedJobsPermanentlyTaskTest extends AbstractZooKeeperTaskTest {

    @Captor
    ArgumentCaptor<Instant> updatedBeforeCaptor;

    DeleteDeletedJobsPermanentlyTask task;

    @BeforeEach
    void setUpTask() {
        task = new DeleteDeletedJobsPermanentlyTask(backgroundJobServer);
    }

    @Override
    protected BackgroundJobServerConfiguration getBackgroundJobServerConfiguration() {
        return super.getBackgroundJobServerConfiguration().andPermanentlyDeleteDeletedJobsAfter(ofHours(12));
    }

    @Test
    void testTask() {
        runTask(task);

        verify(storageProvider).deleteJobsPermanently(eq(DELETED), updatedBeforeCaptor.capture());

        assertThat(updatedBeforeCaptor.getValue()).isCloseTo(now().minus(ofHours(12)), within(500, ChronoUnit.MILLIS));
    }
}