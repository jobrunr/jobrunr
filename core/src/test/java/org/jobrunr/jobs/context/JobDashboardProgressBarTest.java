package org.jobrunr.jobs.context;

import org.jobrunr.jobs.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;

class JobDashboardProgressBarTest {

    private JobDashboardProgressBar jobDashboardProgressBar;

    @BeforeEach
    void setUpJobDashboardProgressBar() {
        Job job = aJobInProgress().build();
        jobDashboardProgressBar = new JobDashboardProgressBar(job, 10L);
    }

    @Test
    void canSetProgress() {
        jobDashboardProgressBar.setProgress(3);

        assertThat(jobDashboardProgressBar.getSucceededAmount()).isEqualTo(3);
        assertThat(jobDashboardProgressBar.getProgressAsPercentage()).isEqualTo(30);
        assertThat(jobDashboardProgressBar.getProgressAsRatio()).isEqualTo(0.3);
    }

    @Test
    void canIncrementSucceeded() {
        jobDashboardProgressBar.incrementSucceeded();

        assertThat(jobDashboardProgressBar.getSucceededAmount()).isEqualTo(1);
        assertThat(jobDashboardProgressBar.getFailedAmount()).isEqualTo(0);
        assertThat(jobDashboardProgressBar.getProgressAsPercentage()).isEqualTo(10);
        assertThat(jobDashboardProgressBar.getProgressAsRatio()).isEqualTo(0.1);
    }

    @Test
    void canIncrementFailed() {
        jobDashboardProgressBar.incrementFailed();

        assertThat(jobDashboardProgressBar.getSucceededAmount()).isEqualTo(0);
        assertThat(jobDashboardProgressBar.getFailedAmount()).isEqualTo(1);
        assertThat(jobDashboardProgressBar.getProgressAsPercentage()).isEqualTo(0);
        assertThat(jobDashboardProgressBar.getProgressAsRatio()).isEqualTo(0);
    }

    @Test
    void canConstructProgressBarWithSize0() {
        final Job job = aJobInProgress().build();

        JobDashboardProgressBar jobDashboardProgressBar = new JobDashboardProgressBar(job, 0L);
        assertThat(jobDashboardProgressBar.getTotalAmount()).isEqualTo(0L);
        assertThat(jobDashboardProgressBar.getSucceededAmount()).isEqualTo(0L);
        assertThat(jobDashboardProgressBar.getProgressAsPercentage()).isEqualTo(100);
        assertThat(jobDashboardProgressBar.getProgressAsRatio()).isEqualTo(1);
    }

    @Test
    void canNotConstructProgressBarWithNegativeSize() {
        final Job job = aJobInProgress().build();
        assertThatThrownBy(() -> new JobDashboardProgressBar(job, -10L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void doesNotThrowExceptionIfNoJobProgressBarIsPresent() {
        final Job job = aJobInProgress().build();
        assertThatCode(() -> JobDashboardProgressBar.get(job)).doesNotThrowAnyException();

        assertThat(JobDashboardProgressBar.get(job)).isNull();
    }
}