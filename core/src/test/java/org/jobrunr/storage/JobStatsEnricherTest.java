package org.jobrunr.storage;

import org.assertj.core.data.Offset;
import org.jobrunr.utils.SleepUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.time.Duration;
import java.time.Instant;

import static java.time.Instant.now;
import static org.jobrunr.JobRunrAssertions.assertThat;

class JobStatsEnricherTest {

    private JobStatsEnricher jobStatsEnricher;

    @BeforeEach
    void setUpJobStatsEnricher() {
        jobStatsEnricher = new JobStatsEnricher();
    }

    @Test
    void enrichGivenNoPreviousJobStatsAndNoWorkToDo() {
        JobStatsExtended extendedJobStats = jobStatsEnricher.enrich(getJobStats(0L, 0L, 0L, 0L));

        assertThat(extendedJobStats.getAmountSucceeded()).isZero();
        assertThat(extendedJobStats.getAmountFailed()).isZero();
        assertThat(extendedJobStats.getEstimation().isProcessingDone()).isTrue();
        assertThat(extendedJobStats.getEstimation().isEstimatedProcessingFinishedInstantAvailable()).isFalse();
    }

    @Test
    void enrichGivenNoPreviousJobStatsAndWorkToDoEnqueuedAndProcessing() {
        JobStatsExtended extendedJobStats = jobStatsEnricher.enrich(getJobStats(1L, 1L, 0L, 0L));

        assertThat(extendedJobStats.getAmountSucceeded()).isZero();
        assertThat(extendedJobStats.getAmountFailed()).isZero();
        assertThat(extendedJobStats.getEstimation().isProcessingDone()).isFalse();
        assertThat(extendedJobStats.getEstimation().isEstimatedProcessingFinishedInstantAvailable()).isFalse();
    }

    @Test
    void enrichGivenNoPreviousJobStatsAndWorkToDoProcessing() {
        JobStatsExtended extendedJobStats = jobStatsEnricher.enrich(getJobStats(0L, 1L, 0L, 0L));

        assertThat(extendedJobStats.getAmountSucceeded()).isZero();
        assertThat(extendedJobStats.getAmountFailed()).isZero();
        assertThat(extendedJobStats.getEstimation().isProcessingDone()).isFalse();
        assertThat(extendedJobStats.getEstimation().isEstimatedProcessingFinishedInstantAvailable()).isFalse();
    }

    @Test
    void firstRelevantJobStatsIsSetInitially() {
        JobStats firstJobStats = getJobStats(0L, 0L, 0L, 100L);
        jobStatsEnricher.enrich(firstJobStats);

        JobStats jobStats = Whitebox.getInternalState(jobStatsEnricher, "firstRelevantJobStats");
        assertThat(jobStats).isEqualToComparingFieldByField(firstJobStats);
    }

    @Test
    void firstRelevantJobStatsIsUpdated() {
        JobStats firstJobStats = getJobStats(0L, 0L, 0L, 100L);
        JobStats secondJobStats = getJobStats(10L, 0L, 0L, 100L);
        jobStatsEnricher.enrich(firstJobStats);
        jobStatsEnricher.enrich(secondJobStats);

        JobStats jobStats = Whitebox.getInternalState(jobStatsEnricher, "firstRelevantJobStats");
        assertThat(jobStats).isEqualToComparingFieldByField(secondJobStats);
    }

    @Test
    void firstRelevantJobStatsIsUpdatedAfterWorkIsDone() {
        JobStats firstJobStats = getJobStats(0L, 0L, 0L, 100L);
        jobStatsEnricher.enrich(firstJobStats);
        SleepUtils.sleep(2); //sleeping as JVM is too fast and runs code in the same nanosecond

        JobStats secondJobStats = getJobStats(10L, 0L, 0L, 100L);
        jobStatsEnricher.enrich(secondJobStats);
        SleepUtils.sleep(2); //sleeping as JVM is too fast and runs code in the same nanosecond

        JobStats thirdJobStats = getJobStats(0L, 0L, 0L, 110L);
        jobStatsEnricher.enrich(thirdJobStats);
        SleepUtils.sleep(2); //sleeping as JVM is too fast and runs code in the same nanosecond

        JobStats jobStats = Whitebox.getInternalState(jobStatsEnricher, "firstRelevantJobStats");
        assertThat(jobStats).isEqualToComparingFieldByField(thirdJobStats);
    }

    @Test
    void estimatedTimeProcessingIsCalculated1() {
        JobStats firstJobStats = getJobStats(now().minusSeconds(10), 100L, 0L, 0L, 100L);
        JobStats secondJobStats = getJobStats(now(), 85L, 5L, 0L, 110L);
        JobStatsExtended secondJobStatsExtended = enrich(firstJobStats, secondJobStats);

        assertThat(secondJobStatsExtended.getEstimation().isProcessingDone()).isFalse();
        assertThat(Duration.between(now(), secondJobStatsExtended.getEstimation().getEstimatedProcessingFinishedAt()).toSeconds()).isCloseTo(90L, Offset.offset(1L));
    }

    @Test
    void estimatedTimeProcessingIsCalculated2() {
        JobStats jobStats0 = getJobStats(now().minusSeconds(60), 100L, 0L, 0L, 100L);
        JobStats jobStats1 = getJobStats(now().minusSeconds(50), 85L, 5L, 0L, 110L);
        JobStats jobStats2 = getJobStats(now().minusSeconds(40), 75L, 5L, 0L, 120L);
        JobStats jobStats3 = getJobStats(now().minusSeconds(30), 65L, 5L, 0L, 130L);
        JobStats jobStats4 = getJobStats(now().minusSeconds(20), 55L, 5L, 0L, 140L);
        JobStats jobStats5 = getJobStats(now().minusSeconds(10), 45L, 5L, 0L, 150L);
        JobStats jobStats6 = getJobStats(now(), 35L, 5L, 0L, 160L);

        JobStatsExtended jobStatsExtended = enrich(jobStats0, jobStats1, jobStats2, jobStats3, jobStats4, jobStats5, jobStats6);

        assertThat(jobStatsExtended.getEstimation().isProcessingDone()).isFalse();
        assertThat(Duration.between(now(), jobStatsExtended.getEstimation().getEstimatedProcessingFinishedAt()).toSeconds()).isCloseTo(40L, Offset.offset(1L));
    }

    @Test
    void estimatedTimeProcessingIsCalculated3() {
        JobStats firstJobStats = getJobStats(now().minusMillis(10), 100L, 0L, 0L, 100L);
        JobStats secondJobStats = getJobStats(now(), 99L, 0L, 0L, 101L);

        JobStatsExtended jobStatsExtended = enrich(firstJobStats, secondJobStats);

        assertThat(jobStatsExtended.getEstimation().isProcessingDone()).isFalse();
        assertThat(jobStatsExtended.getEstimation().isEstimatedProcessingFinishedInstantAvailable()).isFalse();
    }

    @Test
    void estimatedTimeProcessingIsCalculated4() {
        JobStats firstJobStats = getJobStats(now().minusSeconds(10), 100L, 0L, 0L, 100L);
        JobStats secondJobStats = getJobStats(now(), 99L, 0L, 0L, 101L);

        JobStatsExtended jobStatsExtended = enrich(firstJobStats, secondJobStats);

        assertThat(jobStatsExtended.getEstimation().isProcessingDone()).isFalse();
        assertThat(jobStatsExtended.getEstimation().isEstimatedProcessingFinishedInstantAvailable()).isTrue();
        assertThat(Duration.between(now(), jobStatsExtended.getEstimation().getEstimatedProcessingFinishedAt()).toSeconds()).isCloseTo(990L, Offset.offset(10L));
    }

    @Test
    void estimatedTimeProcessingIsCalculated5() {
        JobStats firstJobStats = getJobStats(now().minusSeconds(3610), 10L, 0L, 0L, 100L);
        JobStats secondJobStats = getJobStats(now().minusSeconds(3600), 5L, 4L, 0L, 101L);
        JobStats thirdJobStats = getJobStats(now(), 4L, 4L, 0L, 102L);

        JobStatsExtended jobStatsExtended = enrich(firstJobStats, secondJobStats, thirdJobStats);

        assertThat(jobStatsExtended.getEstimation().isProcessingDone()).isFalse();
        assertThat(jobStatsExtended.getEstimation().isEstimatedProcessingFinishedInstantAvailable()).isTrue();
        assertThat(Duration.between(now(), jobStatsExtended.getEstimation().getEstimatedProcessingFinishedAt()).toSeconds()).isCloseTo(80L, Offset.offset(2L));
    }

    private JobStatsExtended enrich(JobStats... allJobStats) {
        JobStatsExtended lastJobStatsExtends = null;
        for (JobStats jobStats : allJobStats) {
            lastJobStatsExtends = jobStatsEnricher.enrich(jobStats);
        }
        return lastJobStatsExtends;
    }

    private JobStats getJobStats(long enqueued, long processing, long failed, long succeeded) {
        return getJobStats(now(), enqueued, processing, failed, succeeded);
    }

    private JobStats getJobStats(Instant instant, long enqueued, long processing, long failed, long succeeded) {
        return new JobStats(instant, 0L, 0L, enqueued, processing, failed, succeeded, 0L, 0L, 1, 1);
    }

}