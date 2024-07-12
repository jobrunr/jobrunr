package org.jobrunr.server.tasks.zookeeper;

import org.assertj.core.api.Assertions;
import org.jobrunr.jobs.Job;
import org.jobrunr.server.carbonaware.CarbonAwareJobManager;
import org.jobrunr.server.tasks.AbstractTaskTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.jobrunr.jobs.JobTestBuilder.aCarbonAwaitingJob;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.DatetimeMocker.mockZonedDateTime;
import static org.mockito.InstantMocker.mockTime;
import static org.mockito.LocalDateMocker.mockLocalDate;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessCarbonAwareAwaitingJobsTaskTest extends AbstractTaskTest {
    private ProcessCarbonAwareAwaitingJobsTask task;
    @Mock
    CarbonAwareJobManager carbonAwareJobManager;
    @Captor
    ArgumentCaptor<Instant> instantArgumentCaptor;

    private static final ZoneId ZONEID = ZoneId.of("Europe/Brussels");

    @BeforeEach
    void setUp() {
        task = new ProcessCarbonAwareAwaitingJobsTask(backgroundJobServer);
    }

    @Override
    protected CarbonAwareJobManager setUpCarbonAwareJobManager() {
        return carbonAwareJobManager;
    }

    @Test
    void testCallsCarbonAwareJobManagerMoveToNextState() {
        Job job = aCarbonAwaitingJob().build();

        when(storageProvider.getCarbonAwareJobList(any(), any())).thenReturn(List.of(job));

        runTask(task);

        verify(carbonAwareJobManager).moveToNextState(job);
    }

    @Test
    void taskRetrievesCarbonAwareJobsBeforeComputedInstant() {
        try (MockedStatic<Instant> ignored1 = mockTime(Instant.parse("2024-07-01T14:00:00Z"));
             MockedStatic<LocalDate> ignored2 = mockLocalDate(LocalDate.parse("2024-07-01"));
             MockedStatic<ZonedDateTime> ignored3 = mockZonedDateTime(ZonedDateTime.parse("2024-07-01T16:00:00+02:00[Europe/Brussels]"), ZONEID)) {
            runTask(task);

            verify(storageProvider).getCarbonAwareJobList(instantArgumentCaptor.capture(), any());
            Assertions.assertThat(instantArgumentCaptor.getValue()).isEqualTo(Instant.now());
        }

        clearInvocations(storageProvider);

        try (MockedStatic<Instant> ignored1 = mockTime(Instant.parse("2024-07-01T17:07:34Z"));
             MockedStatic<LocalDate> ignored2 = mockLocalDate(LocalDate.parse("2024-07-01"));
             MockedStatic<ZonedDateTime> ignored3 = mockZonedDateTime(ZonedDateTime.parse("2024-07-01T19:07:34+02:00[Europe/Brussels]"), ZONEID)) {
            runTask(task);

            verify(storageProvider).getCarbonAwareJobList(instantArgumentCaptor.capture(), any());
            Assertions.assertThat(instantArgumentCaptor.getValue()).isEqualTo(Instant.now());
        }

        clearInvocations(storageProvider);

        try (MockedStatic<Instant> ignored1 = mockTime(Instant.parse("2024-07-01T17:07:36Z"));
             MockedStatic<LocalDate> ignored2 = mockLocalDate(LocalDate.parse("2024-07-01"));
             MockedStatic<ZonedDateTime> ignored3 = mockZonedDateTime(ZonedDateTime.parse("2024-07-01T19:07:36+02:00[Europe/Brussels]"), ZONEID)) {
            runTask(task);

            verify(storageProvider).getCarbonAwareJobList(instantArgumentCaptor.capture(), any());
            Assertions.assertThat(instantArgumentCaptor.getValue()).isEqualTo("2024-07-02T21:59:59.999999999Z");
        }

        clearInvocations(storageProvider);

        try (MockedStatic<Instant> ignored1 = mockTime(Instant.parse("2024-07-01T20:07:36Z"));
             MockedStatic<LocalDate> ignored2 = mockLocalDate(LocalDate.parse("2024-07-01"));
             MockedStatic<ZonedDateTime> ignored3 = mockZonedDateTime(ZonedDateTime.parse("2024-07-01T21:07:36+02:00[Europe/Brussels]"), ZONEID)) {
            runTask(task);

            verify(storageProvider).getCarbonAwareJobList(instantArgumentCaptor.capture(), any());
            Assertions.assertThat(instantArgumentCaptor.getValue()).isEqualTo("2024-07-02T21:59:59.999999999Z");
        }

        clearInvocations(storageProvider);

        try (MockedStatic<Instant> ignored1 = mockTime(Instant.parse("2024-07-01T22:07:36Z"));
             MockedStatic<LocalDate> ignored2 = mockLocalDate(LocalDate.parse("2024-07-01"));
             MockedStatic<ZonedDateTime> ignored3 = mockZonedDateTime(ZonedDateTime.parse("2024-07-01T00:07:36+02:00[Europe/Brussels]"), ZONEID)) {
            runTask(task);

            verify(storageProvider).getCarbonAwareJobList(instantArgumentCaptor.capture(), any());
            Assertions.assertThat(instantArgumentCaptor.getValue()).isEqualTo(Instant.now());
        }
    }
}
