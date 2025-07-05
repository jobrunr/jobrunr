package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobAssert;
import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.carbonaware.CarbonAwareApiWireMockExtension;
import org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfiguration;
import org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfigurationReader;
import org.jobrunr.server.carbonaware.CarbonIntensityApiClient;
import org.jobrunr.server.carbonaware.CarbonIntensityForecast;
import org.jobrunr.server.carbonaware.CarbonIntensityForecast.TimestampedCarbonIntensityForecast;
import org.jobrunr.server.tasks.AbstractTaskTest;
import org.jobrunr.storage.JobRunrMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedStaticHolder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;

import static java.time.Instant.now;
import static java.time.Instant.parse;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.server.carbonaware.CarbonApiMockResponses.BELGIUM_2024_07_11;
import static org.jobrunr.server.carbonaware.CarbonApiMockResponses.BELGIUM_PARTIAL_2024_07_11_FULL_2024_07_12;
import static org.jobrunr.server.carbonaware.CarbonApiMockResponses.BELGIUM_PARTIAL_2024_07_12;
import static org.jobrunr.server.carbonaware.CarbonApiMockResponses.GERMANY_2024_07_11;
import static org.jobrunr.server.carbonaware.CarbonApiMockResponses.ITALY_2025_05_20_PT15M;
import static org.jobrunr.server.carbonaware.CarbonApiMockResponses.UNKNOWN_AREA;
import static org.jobrunr.server.carbonaware.CarbonAwareApiWireMockExtension.buildForecastSlots;
import static org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfiguration.usingDisabledCarbonAwareJobProcessingConfiguration;
import static org.mockito.InstantMocker.mockTime;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;

class ProcessCarbonAwareAwaitingJobsTaskTest extends AbstractTaskTest {

    @RegisterExtension
    static CarbonAwareApiWireMockExtension carbonAwareApiMock = new CarbonAwareApiWireMockExtension();

    @Test
    void runTaskWithCarbonAwareDisabledDoesNotUpdateCarbonIntensityForecast() {
        ProcessCarbonAwareAwaitingJobsTask task = createProcessCarbonAwareAwaitingJobsTask(usingDisabledCarbonAwareJobProcessingConfiguration());

        runTask(task);

        verify(carbonIntensityApiClient(task), times(0)).fetchCarbonIntensityForecast();
    }

    @Test
    void runTaskWithCarbonAwareDisabledSchedulesCarbonAwaitingJobsAtBeginningOfCarbonAwarePeriod() {
        // GIVEN
        ProcessCarbonAwareAwaitingJobsTask task = createProcessCarbonAwareAwaitingJobsTask(usingDisabledCarbonAwareJobProcessingConfiguration());
        ZonedDateTime currentTime = ZonedDateTime.now().truncatedTo(MINUTES);
        try (MockedStaticHolder ignored = mockTime(currentTime)) {
            Job job = storageProvider.save(aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now(), now().plus(6, HOURS))).build());

            // WHEN
            runTask(task);

            // THEN
            verify(carbonIntensityApiClient(task), times(0)).fetchCarbonIntensityForecast();
            assertThatJob(job)
                    .hasStates(AWAITING, SCHEDULED)
                    .hasScheduledAt(now(), "Carbon aware scheduling is disabled, scheduling job at " + Instant.now());  // from is fallback instant
        }
    }

    @Test
    void runTaskWithUnexpectedCarbonIntensityForecastValuesStillSchedulesJobAtFallbackInstant() {
        // GIVEN
        ZonedDateTime currentTime = ZonedDateTime.now();
        try (MockedStaticHolder ignored = mockTime(currentTime)) {
            ProcessCarbonAwareAwaitingJobsTask task = createProcessCarbonAwareAwaitingJobsTask("BE");
            carbonAwareApiMock.mockResponseWhenRequestingAreaCode("BE", new CarbonIntensityForecast(
                    new CarbonIntensityForecast.ApiResponseStatus("OK", "message"),
                    "dataProvider",
                    "dataIdentifier",
                    "displayName",
                    null,
                    Instant.now().plus(1, DAYS),
                    null, // Unexpected: this causes a NullPointerException
                    List.of(new TimestampedCarbonIntensityForecast(Instant.now(), Instant.now().plus(1, HOURS), 123)))
            );
            var job = storageProvider.save(aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now(), now().plus(6, HOURS))).build());

            // WHEN
            runTask(task);

            // THEN
            verify(carbonIntensityApiClient(task)).fetchCarbonIntensityForecast();
            assertThatJob(job)
                    .hasStates(AWAITING, SCHEDULED)
                    .hasScheduledAt(now(), "Unexpected problem scheduling the carbon aware job, scheduling at " + Instant.now()); // from is fallback instant
        }
    }

    @Test
    void taskCallsCarbonIntensityApiClientAndSchedulesCarbonAwaitingJobAtIdealMoment() {
        // GIVEN
        ZonedDateTime currentTime = ZonedDateTime.now();
        try (MockedStaticHolder ignored = mockTime(currentTime)) {
            ProcessCarbonAwareAwaitingJobsTask task = createProcessCarbonAwareAwaitingJobsTask("BE");
            carbonAwareApiMock.mockDefaultResponseWhenRequestingAreaCode("BE");
            Job job = storageProvider.save(aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now(), now().plus(6, HOURS))).build());

            // WHEN
            runTask(task);

            // THEN
            verify(carbonIntensityApiClient(task)).fetchCarbonIntensityForecast();
            assertThatJob(job)
                    .hasStates(AWAITING, SCHEDULED)
                    .hasScheduledAt(now().plus(1, HOURS).truncatedTo(HOURS), "At the best moment to minimize carbon impact in MOCK_AREA");
        }
    }

    @Test
    void taskCallsCarbonIntensityApiClientAndIfOnlyOutdatedCarbonIntensityForecastAvailableSchedulesJobImmediately() {
        // GIVEN
        try (MockedStaticHolder ignored = mockTime(ZonedDateTime.parse("2025-05-25T09:00:00Z"))) { // in the past
            carbonAwareApiMock.mockDefaultResponseWhenRequestingAreaCode("BE");
        }

        try (MockedStaticHolder ignored = mockTime(ZonedDateTime.parse("2025-05-27T09:00:00Z"))) {
            ProcessCarbonAwareAwaitingJobsTask task = createProcessCarbonAwareAwaitingJobsTask("BE");
            Job job = storageProvider.save(aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now(), now().plus(6, HOURS))).build());

            // WHEN
            runTask(task);

            // THEN
            verify(carbonIntensityApiClient(task)).fetchCarbonIntensityForecast();
            assertThatJob(job)
                    .hasStates(AWAITING, SCHEDULED)
                    .hasScheduledAt(now()); // from is fallback instant
        }
    }

    @Test
    void taskCallsCarbonIntensityApiClientAndIfNoCarbonIntensityForecastAvailableSchedulesJobImmediatelyIfCarbonAwarePeriodIsBeforeTheRefreshTime() {
        // GIVEN
        try (MockedStaticHolder ignored = mockTime(ZonedDateTime.parse("2025-05-27T09:00:00Z"))) { // daily refresh time is at 19h if no data
            ProcessCarbonAwareAwaitingJobsTask task = createProcessCarbonAwareAwaitingJobsTask("BE");
            Job job = storageProvider.save(aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now(), now().plus(6, HOURS))).build());

            // WHEN
            runTask(task);

            // THEN
            verify(carbonIntensityApiClient(task)).fetchCarbonIntensityForecast();
            assertThatJob(job)
                    .hasStates(AWAITING, SCHEDULED)
                    .hasScheduledAt(now()); // from is fallback instant
        }
    }

    @Test
    void taskCallsCarbonIntensityApiClientAndIfNoCarbonIntensityForecastAvailableKeepsAwaitingStateIfCarbonAwarePeriodIsAfterTheRefreshTime() {
        // GIVEN
        try (MockedStaticHolder ignored = mockTime(ZonedDateTime.parse("2025-05-27T17:00:00Z"))) { // daily refresh time is at 19h if no data
            ProcessCarbonAwareAwaitingJobsTask task = createProcessCarbonAwareAwaitingJobsTask("BE");
            Job job = storageProvider.save(aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now(), now().plus(6, HOURS))).build());

            // WHEN
            runTask(task);

            // THEN
            verify(carbonIntensityApiClient(task)).fetchCarbonIntensityForecast();
            assertThatJob(job).hasStates(AWAITING);
            assertThat(storageProvider.getMetadata("CarbonIntensityApiErrorNotification")).isNotEmpty();
        }
    }

    @Test
    void taskCallsCarbonIntensityApiClientAndSchedulesCarbonAwaitingJobsImmediatelyThatHaveDeadlineInThePast() {
        // GIVEN
        LocalDate localDate = LocalDate.of(2024, 7, 11);
        try (var ignored = mockTime(startOfDay(localDate))) {
            ProcessCarbonAwareAwaitingJobsTask task = createProcessCarbonAwareAwaitingJobsTask("BE");
            carbonAwareApiMock.mockResponseWhenRequestingAreaCode("BE", BELGIUM_PARTIAL_2024_07_12);
            Job job = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now().minus(8, HOURS), now().minus(4, HOURS))).build();
            saveJobsInStorageProvider(job);

            // WHEN
            runTask(task);

            // THEN
            verify(carbonIntensityApiClient(task)).fetchCarbonIntensityForecast();
            assertThatJob(job)
                    .hasStates(AWAITING, SCHEDULED)
                    .hasScheduledAt(now(), "Passed its deadline, scheduling now.");
        }
    }

    @Test
    void taskCallsCarbonIntensityApiClientAndSchedulesCarbonAwaitingJobsImmediatelyIfMarginIsSmallerThanMinimumScheduleMargin() {
        // GIVEN
        LocalDate localDate = LocalDate.of(2024, 7, 11);
        try (var ignored = mockTime(startOfDay(localDate))) {
            carbonAwareApiMock.mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_07_11);
            ProcessCarbonAwareAwaitingJobsTask task = createProcessCarbonAwareAwaitingJobsTask("BE");
            Job job = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.before(now().plusSeconds(300))).build();
            saveJobsInStorageProvider(job);

            // WHEN
            runTask(task);

            // THEN
            verify(carbonIntensityApiClient(task)).fetchCarbonIntensityForecast();
            assertThatJob(job)
                    .hasStates(AWAITING, SCHEDULED)
                    .hasScheduledAt(now(), "Not enough margin (PT5M) to be scheduled carbon aware.");
        }
    }

    @Test
    void taskMovesCarbonAwaitingJobsToNextState() {
        ZonedDateTime currentTime = ZonedDateTime.now();
        try (MockedStaticHolder ignored = mockTime(currentTime)) {
            ProcessCarbonAwareAwaitingJobsTask task = createProcessCarbonAwareAwaitingJobsTask("BE");
            carbonAwareApiMock.mockDefaultResponseWhenRequestingAreaCode("BE");

            List<Job> jobs = storageProvider.save(List.of(
                    aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now(), now().plus(4, HOURS))).build(),
                    aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now().plus(2, HOURS), now().plus(4, HOURS)), "schedule margin too small").build(),
                    aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now().plus(4, HOURS), now().plus(8, HOURS))).build(),
                    aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now().plus(12, HOURS), now().plus(16, HOURS))).build(),
                    aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now().plus(36, HOURS), now().plus(48, HOURS)), "scheduled carbon-aware too far in the future").build()
            ));

            runTask(task);

            assertThatJob(jobs, 0)
                    .hasStates(AWAITING, SCHEDULED)
                    .hasScheduledAt(now().plus(1, HOURS).truncatedTo(HOURS));
            assertThatJob(jobs, 1)
                    .hasStates(AWAITING, SCHEDULED)
                    .hasScheduledAt(now().plus(2, HOURS));
            assertThatJob(jobs, 2)
                    .hasStates(AWAITING, SCHEDULED)
                    .hasScheduledAt(now().plus(5, HOURS).truncatedTo(HOURS));
            assertThatJob(jobs, 3)
                    .hasStates(AWAITING, SCHEDULED)
                    .hasScheduledAt(now().plus(13, HOURS).truncatedTo(HOURS));
            assertThatJob(jobs, 4)
                    .hasStates(AWAITING);
        }
    }

    @Test
    void taskTimezoneReturnsSystemZoneIdByDefaultIfNoForecastAvailable() {
        ProcessCarbonAwareAwaitingJobsTask task = createProcessCarbonAwareAwaitingJobsTask("BE");

        assertThat(task.getTimeZone()).isEqualTo(ZoneId.systemDefault());
    }

    @Test
    void taskGetAvailableForecastEndTimeReturnsNextRefreshTimeIfForecastIsNotAvailable() {
        ProcessCarbonAwareAwaitingJobsTask task = createProcessCarbonAwareAwaitingJobsTask("DE");

        assertThat(task.getAvailableForecastEndTime()).isCloseTo(now(), within(1, SECONDS));
    }

    @Test
    void taskTimeZoneReturnsCarbonIntensityForecastTimezoneIfAvailable() {
        // GIVEN
        carbonAwareApiMock.mockResponseWhenRequestingAreaCode("DE", GERMANY_2024_07_11);
        ProcessCarbonAwareAwaitingJobsTask task = createProcessCarbonAwareAwaitingJobsTask("DE");

        // WHEN
        runTask(task);

        // THEN
        assertThat(task.getTimeZone()).isEqualTo(ZoneId.of("Europe/Berlin"));
    }

    @Test
    void taskGetAvailableForecastEndTimeReturnsForecastEndPeriodWhenItsLaterThanNextRefreshTime() {
        try (var ignored = mockTime(startOfDay(LocalDate.of(2024, 7, 11)))) {
            // GIVEN
            carbonAwareApiMock.mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_07_11);
            ProcessCarbonAwareAwaitingJobsTask task = createProcessCarbonAwareAwaitingJobsTask("BE");

            // WHEN
            runTask(task);

            // THEN
            assertThat(task.getAvailableForecastEndTime()).isEqualTo("2024-07-11T22:00:00Z");
        }
    }

    @Test
    void taskGetAvailableForecastEndTimeReturnsNextRefreshTimeWhenItsLaterThanForecastEndPeriod() {
        // GIVEN
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(startOfDay(LocalDate.of(2024, 7, 12)), ZoneId.systemDefault());
        try (var ignored = mockTime(zonedDateTime)) {
            carbonAwareApiMock.mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_07_11);
            ProcessCarbonAwareAwaitingJobsTask task = createProcessCarbonAwareAwaitingJobsTask("BE");

            // WHEN
            runTask(task);

            // THEN
            assertThat(task.getAvailableForecastEndTime()).isAfter(parse("2024-07-12T17:00:00Z"));
        }
    }

    @Test
    void taskUpdateCarbonIntensityForecastIfNecessaryTakesIntoAccountNextForecastAvailableAtWhenSettingNextRefreshTime() {
        try (var ignored = mockTime(startOfDay(LocalDate.of(2024, 7, 11)))) {
            // GIVEN
            carbonAwareApiMock.mockResponseWhenRequestingAreaCode("BE", BELGIUM_PARTIAL_2024_07_11_FULL_2024_07_12);
            ProcessCarbonAwareAwaitingJobsTask task = createProcessCarbonAwareAwaitingJobsTask("BE");

            // THEN
            assertThat(nextRefreshTime(task)).isEqualTo(now());

            // WHEN
            runTask(task);

            // THEN
            verify(carbonIntensityApiClient(task)).fetchCarbonIntensityForecast();
            assertThat(nextRefreshTime(task)).isCloseTo("2024-07-12T16:30:00.873318Z", within(randomRefreshTime(task)));
        }
    }

    @Test
    void taskUpdateCarbonIntensityForecastIfNecessaryIsCachedUntilNextRefreshTime() {
        ZonedDateTime dateTime = ZonedDateTime.now().truncatedTo(HOURS).withHour(17);
        try (var ignored = mockTime(dateTime)) {
            // GIVEN
            carbonAwareApiMock.mockResponseWhenRequestingAreaCode("DE", UNKNOWN_AREA);
            ProcessCarbonAwareAwaitingJobsTask task = createProcessCarbonAwareAwaitingJobsTask("DE");

            // THEN
            assertThat(nextRefreshTime(task)).isEqualTo(now());

            // WHEN
            runTask(task);

            // THEN
            verify(carbonIntensityApiClient(task), times(1)).fetchCarbonIntensityForecast();
            assertThat(nextRefreshTime(task)).isCloseTo(dateTime.withHour(19).toInstant(), within(randomRefreshTime(task)));

            // WHEN
            clearInvocations(carbonIntensityApiClient(task));
            runTask(task);

            // THEN
            verify(carbonIntensityApiClient(task), never()).fetchCarbonIntensityForecast();
        }
    }

    @Test
    void taskUpdateCarbonIntensityForecastIfNecessarySetsNextRefreshTimeTheNextDayIfRunningAfterPlannedDailyRefreshTime() {
        ZonedDateTime dateTime = ZonedDateTime.now().truncatedTo(HOURS).withHour(20);
        try (var ignored = mockTime(dateTime)) {
            // GIVEN
            carbonAwareApiMock.mockResponseWhenRequestingAreaCode("DE", UNKNOWN_AREA);
            ProcessCarbonAwareAwaitingJobsTask task = createProcessCarbonAwareAwaitingJobsTask("DE");

            // THEN
            assertThat(nextRefreshTime(task)).isEqualTo(now());

            // WHEN
            runTask(task);

            verify(carbonIntensityApiClient(task), times(1)).fetchCarbonIntensityForecast();

            assertThat(nextRefreshTime(task)).isCloseTo(dateTime.plusDays(1).withHour(19).toInstant(), within(randomRefreshTime(task)));
        }
    }

    @Test
    void taskUpdateCarbonIntensityForecastDeletesOldForecastMetadataAndUpdatesNewlyFetchedOnes() {
        var currentTime = ZonedDateTime.now();
        carbonAwareApiMock.mockResponseWhenRequestingAreaCode("BE", createCarbonIntensityForecast(List.of(new TimestampedCarbonIntensityForecast(Instant.now(), Instant.now().plus(1, HOURS), 123)), Duration.ofHours(1)));

        try (MockedStaticHolder ignored = mockTime(currentTime)) {
            // stale data to delete
            storageProvider.saveMetadata(new JobRunrMetadata("carbon-intensity-forecast", currentTime.minus(10, DAYS).toLocalDate().toString(), backgroundJobServer.getJsonMapper().serialize(createCarbonIntensityForecast(List.of(new TimestampedCarbonIntensityForecast(Instant.now(), Instant.now().plus(1, HOURS), 123)), Duration.ofHours(1)))));
            storageProvider.saveMetadata(new JobRunrMetadata("carbon-intensity-forecast", currentTime.minus(8, DAYS).toLocalDate().toString(), backgroundJobServer.getJsonMapper().serialize(createCarbonIntensityForecast(List.of(new TimestampedCarbonIntensityForecast(Instant.now(), Instant.now().plus(1, HOURS), 123)), Duration.ofHours(1)))));
            // still in use
            storageProvider.saveMetadata(new JobRunrMetadata("carbon-intensity-forecast", currentTime.minus(1, DAYS).toLocalDate().toString(), backgroundJobServer.getJsonMapper().serialize(createCarbonIntensityForecast(List.of(new TimestampedCarbonIntensityForecast(Instant.now(), Instant.now().plus(1, HOURS), 123)), Duration.ofHours(1)))));

            var task = createProcessCarbonAwareAwaitingJobsTask("BE");

            task.updateCarbonIntensityForecast();
        }

        var refreshedMetaData = storageProvider.getMetadata("carbon-intensity-forecast").stream()
                .sorted(Comparator.comparing(JobRunrMetadata::getOwner))
                .collect(toList());
        assertThat(refreshedMetaData).hasSize(2);
        assertThat(refreshedMetaData.get(0).getOwner()).isEqualTo(currentTime.minus(1, DAYS).toLocalDate().toString());
        assertThat(refreshedMetaData.get(1).getOwner()).isEqualTo(currentTime.toLocalDate().toString());
    }

    @Test
    void taskUpdateCarbonIntensityForecastAddsOlderForecastIfNotAvailable() {
        var currentTime = ZonedDateTime.now();
        List<TimestampedCarbonIntensityForecast> hourlyForecast = buildForecastSlots(currentTime.minusDays(1).truncatedTo(DAYS), currentTime.plusDays(1).truncatedTo(DAYS), HOURS, i -> i);

        carbonAwareApiMock.mockResponseWhenRequestingAreaCode("BE", createCarbonIntensityForecast(hourlyForecast, Duration.ofHours(1)));
        try (MockedStaticHolder ignored = mockTime(currentTime)) {
            var task = createProcessCarbonAwareAwaitingJobsTask("BE");
            task.updateCarbonIntensityForecast();
        }

        var refreshedMetaData = storageProvider.getMetadata("carbon-intensity-forecast").stream()
                .sorted(Comparator.comparing(JobRunrMetadata::getOwner))
                .collect(toList());
        assertThat(refreshedMetaData)
                .hasSizeGreaterThanOrEqualTo(2)
                .anyMatch(x -> x.getOwner().equals(currentTime.minusDays(1).toLocalDate().toString()))
                .anyMatch(x -> x.getOwner().equals(currentTime.toLocalDate().toString()));
    }


    @Test
    void taskMoveToNextStateThrowsAnExceptionIfGivenJobsThatAreNotCarbonAwaiting() {
        ProcessCarbonAwareAwaitingJobsTask task = createProcessCarbonAwareAwaitingJobsTask("DE");

        Job job = anEnqueuedJob().build();

        assertThatCode(() -> task.moveCarbonAwareJobToNextState(job))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only jobs in CarbonAwaitingState can move to a next state");
    }

    @Test
    void taskMoveToNextStateAlsoWorksForPT15MIntensityIntervals() {
        // GIVEN
        try (var ignored = mockTime(parse("2025-05-20T13:00:00.000Z"))) {
            carbonAwareApiMock.mockResponseWhenRequestingAreaCode("IT", ITALY_2025_05_20_PT15M);
            ProcessCarbonAwareAwaitingJobsTask task = createProcessCarbonAwareAwaitingJobsTask("IT");
            Job job = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now().minus(1, HOURS), now().plus(3, HOURS))).build();
            saveJobsInStorageProvider(job);

            // WHEN
            runTask(task);

            // THEN
            assertThatJob(job)
                    .hasStates(AWAITING, SCHEDULED)
                    .hasScheduledAt(parse("2025-05-20T12:00:00.000Z"), "At the best moment to minimize carbon impact in Italy");
        }
    }

    private Instant startOfDay(LocalDate localDate) {
        return localDate.atStartOfDay(ZoneId.of("Europe/Brussels")).toInstant();
    }

    private JobAssert assertThatJob(List<Job> jobs, int index) {
        return assertThatJob(jobs.get(index));
    }

    private JobAssert assertThatJob(Job job) {
        return assertThat(storageProvider.getJobById(job.getId()));
    }

    private ProcessCarbonAwareAwaitingJobsTask createProcessCarbonAwareAwaitingJobsTask(String areaCode) {
        return createProcessCarbonAwareAwaitingJobsTask(carbonAwareApiMock.getCarbonAwareJobProcessingConfigurationForAreaCode(areaCode));
    }

    private ProcessCarbonAwareAwaitingJobsTask createProcessCarbonAwareAwaitingJobsTask(CarbonAwareJobProcessingConfiguration carbonAwareJobProcessingConfiguration) {
        return new ProcessCarbonAwareAwaitingJobsTask(backgroundJobServer) {
            @Override
            CarbonAwareJobProcessingConfigurationReader getCarbonAwareJobProcessingConfiguration(BackgroundJobServer backgroundJobServer) {
                return new CarbonAwareJobProcessingConfigurationReader(carbonAwareJobProcessingConfiguration);
            }

            @Override
            CarbonIntensityApiClient getCarbonIntensityApiClient(BackgroundJobServer backgroundJobServer) {
                return spy(super.getCarbonIntensityApiClient(backgroundJobServer));
            }
        };
    }

    private CarbonIntensityApiClient carbonIntensityApiClient(ProcessCarbonAwareAwaitingJobsTask task) {
        return getInternalState(task, "carbonIntensityApiClient");
    }

    private Instant nextRefreshTime(ProcessCarbonAwareAwaitingJobsTask task) {
        return getInternalState(task, "nextRefreshTime");
    }

    private Duration randomRefreshTime(ProcessCarbonAwareAwaitingJobsTask task) {
        return getInternalState(task, "randomRefreshTimeOffset");
    }

    private static CarbonIntensityForecast createCarbonIntensityForecast(List<TimestampedCarbonIntensityForecast> forecast, Duration forecastInterval) {
        return new CarbonIntensityForecast(
                new CarbonIntensityForecast.ApiResponseStatus("OK", "message"),
                "dataProvider",
                "dataIdentifier",
                "displayName",
                null,
                Instant.now().plus(1, DAYS),
                forecastInterval,
                forecast);
    }
}
