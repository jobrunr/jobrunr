package org.jobrunr.server.tasks.other;

import org.assertj.core.api.Assertions;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.carbonaware.AbstractCarbonAwareWiremockTest;
import org.jobrunr.utils.carbonaware.CarbonApiMockResponses;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.DatetimeMocker;
import org.mockito.InstantMocker;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.time.ZonedDateTime;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.states.StateName.*;
import static org.jobrunr.utils.carbonaware.CarbonAwarePeriod.between;

public class ProcessCarbonAwareAwaitingJobsTaskTest extends AbstractCarbonAwareWiremockTest {
    private StorageProvider storageProvider;

    @BeforeEach
    void setUp() {
        storageProvider = new InMemoryStorageProvider();
    }

    @Test
    public void testUpdateAwaitingJobs() {
        // GIVEN
        String area = "DE";
        BackgroundJobServer backgroundJobServer = initializeJobRunr(200, area, storageProvider);
        JobId jobId1 = BackgroundJob.scheduleCarbonAware(between("2500-01-01T00:00:00Z", "2500-01-01T23:00:00Z"),
                () -> System.out.println("1. This job should be scheduled at 12:00"));
        JobId jobId2 = BackgroundJob.scheduleCarbonAware(between("2600-01-01T00:00:00Z", "2600-01-01T23:00:00Z"),
                () -> System.out.println("2. This job should wait"));
        JobId jobId3 = BackgroundJob.scheduleCarbonAware(between("2400-01-01T00:00:00Z","2400-01-01T23:00:00Z"),
                () -> System.out.println("3. This job should be run immediately"));
        JobId jobId4 = BackgroundJob.scheduleCarbonAware(between("2500-01-01T15:00:00Z", "2500-01-01T23:00:00Z"),
                () -> System.out.println("4. This job should be scheduled at 22:00"));

        assertThat(storageProvider.getJobById(jobId1)).hasStates(AWAITING);
        assertThat(storageProvider.getJobById(jobId2)).hasStates(AWAITING);
        assertThat(storageProvider.getJobById(jobId3)).hasStates(AWAITING);
        assertThat(storageProvider.getJobById(jobId4)).hasStates(AWAITING);

        ProcessCarbonAwareAwaitingJobsTask processCarbonAwareAwaitingJobsTask = new ProcessCarbonAwareAwaitingJobsTask(backgroundJobServer);
        JobZooKeeper caronAwareManageAwaitingJobsTask = new JobZooKeeper(backgroundJobServer, processCarbonAwareAwaitingJobsTask);
        mockResponseWhenRequestingArea(area, CarbonApiMockResponses.GERMANY_2500_01_01);
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2500-01-01T08:00:00Z");
            MockedStatic<ZonedDateTime> b = DatetimeMocker.mockZonedDateTime(ZonedDateTime.parse("2500-01-01T08:00:00Z"), "Europe/Brussels")) {
            // WHEN
            caronAwareManageAwaitingJobsTask.run();

            // THEN
            Job job1 = storageProvider.getJobById(jobId1);
            assertThat(job1).hasStates(AWAITING, SCHEDULED);
            Assertions.assertThat(((ScheduledState) job1.getJobState()).getScheduledAt()).isEqualTo(Instant.parse("2500-01-01T12:00:00Z"));

            assertThat(storageProvider.getJobById(jobId2)).hasStates(AWAITING);

            await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId3).getState() == SUCCEEDED);
            assertThat(storageProvider.getJobById(jobId3)).hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);

            Job job4 = storageProvider.getJobById(jobId4);
            assertThat(job4).hasStates(AWAITING, SCHEDULED);
            Assertions.assertThat(((ScheduledState) job4.getJobState()).getScheduledAt()).isEqualTo(Instant.parse("2500-01-01T22:00:00Z"));
        }
    }

    @Test
    public void testUpdateAwaitingJobs_withDeadlinein2Days_shouldStayAwaiting() {
        // GIVEN
        String area = "DE";
        BackgroundJobServer backgroundJobServer = initializeJobRunr(200, area, storageProvider);
        JobId jobId = BackgroundJob.scheduleCarbonAware(between("2500-01-01T00:00:00Z", "2500-01-03T23:00:00Z"),
                () -> System.out.println("This job should stay awaiting"));

        assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING);

        ProcessCarbonAwareAwaitingJobsTask processCarbonAwareAwaitingJobsTask = new ProcessCarbonAwareAwaitingJobsTask(backgroundJobServer);
        JobZooKeeper caronAwareManageAwaitingJobsTask = new JobZooKeeper(backgroundJobServer, processCarbonAwareAwaitingJobsTask);
        mockResponseWhenRequestingArea(area, CarbonApiMockResponses.GERMANY_2500_01_01);
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2500-01-01T08:00:00Z");
            MockedStatic<ZonedDateTime> b = DatetimeMocker.mockZonedDateTime(ZonedDateTime.parse("2500-01-01T08:00:00Z"), "Europe/Brussels")) {
            // WHEN
            caronAwareManageAwaitingJobsTask.run();

            // THEN
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING);
        }
    }
}
