package org.jobrunr.jobs.context;

import org.jobrunr.jobs.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;

class JobDashboardProgressBarTest {

    private Job job;
    private JobDashboardProgressBar jobDashboardProgressBar;

    @BeforeEach
    void setUpJobDashboardProgressBar() {
        job = aJobInProgress().build();
        jobDashboardProgressBar = new JobDashboardProgressBar(job, 10L);
    }

    @Test
    public void canIncreaseByOne() {
        jobDashboardProgressBar.increaseByOne();

        assertThat(jobDashboardProgressBar.getProgress()).isEqualTo(10);
    }

    @Test
    public void canSetValue() {
        jobDashboardProgressBar.setValue(3);

        assertThat(jobDashboardProgressBar.getProgress()).isEqualTo(30);
    }

}