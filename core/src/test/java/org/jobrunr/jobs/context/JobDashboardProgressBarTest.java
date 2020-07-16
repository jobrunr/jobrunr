package org.jobrunr.jobs.context;

import org.jobrunr.jobs.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    void canIncreaseByOne() {
        jobDashboardProgressBar.increaseByOne();

        assertThat(jobDashboardProgressBar.getProgress()).isEqualTo(10);
    }

    @Test
    void canSetValue() {
        jobDashboardProgressBar.setValue(3);

        assertThat(jobDashboardProgressBar.getProgress()).isEqualTo(30);
    }

    @Test
    void canNotConstructProgressBarWithSize0() {
        final Job job = aJobInProgress().build();
        assertThatThrownBy(() -> new JobDashboardProgressBar(job, 0L)).isInstanceOf(IllegalArgumentException.class);
    }

}