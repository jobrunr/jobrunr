package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.server.tasks.AbstractZooKeeperTaskTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class DeleteDeletedJobsPermanentlyTaskTest extends AbstractZooKeeperTaskTest {

    DeleteDeletedJobsPermanentlyTask task;

    @BeforeEach
    void setUpTask() {
        task = new DeleteDeletedJobsPermanentlyTask(backgroundJobServer);
    }

    @Test
    void testTask() {
        runTask(task);

        verify(storageProvider).deleteJobsPermanently(eq(DELETED), any());
    }
}