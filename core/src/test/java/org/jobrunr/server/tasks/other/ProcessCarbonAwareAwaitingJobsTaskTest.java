package org.jobrunr.server.tasks.other;

import org.jobrunr.server.tasks.AbstractTaskTest;
import org.junit.jupiter.api.BeforeEach;

class ProcessCarbonAwareAwaitingJobsTaskTest extends AbstractTaskTest {
    private ProcessCarbonAwareAwaitingJobsTask task;

    @BeforeEach
    void setUp() {
        task = new ProcessCarbonAwareAwaitingJobsTask(backgroundJobServer);
    }

    // TODO test ProcessCarbonAwareAwaitingJobsTask
}
