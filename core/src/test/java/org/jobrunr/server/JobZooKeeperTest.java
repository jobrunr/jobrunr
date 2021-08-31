package org.jobrunr.server;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.jobrunr.SevereJobRunrException;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.filters.JobDefaultFilters;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.storage.*;
import org.jobrunr.stubs.TestServiceInterface;
import org.jobrunr.utils.annotations.Because;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.methodThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.aCopyOf;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;
import static org.jobrunr.jobs.JobTestBuilder.aScheduledJob;
import static org.jobrunr.jobs.JobTestBuilder.aSucceededJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.aDefaultBackgroundJobServerStatus;
import static org.jobrunr.storage.PageRequest.ascOnUpdatedAt;
import static org.jobrunr.utils.SleepUtils.sleep;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobZooKeeperTest {

    @Mock
    private StorageProvider storageProvider;
    @Mock
    private BackgroundJobServer backgroundJobServer;
    @Mock
    private WorkDistributionStrategy workDistributionStrategy;
    @Captor
    private ArgumentCaptor<List<Job>> jobsToSaveArgumentCaptor;
    @Captor
    private ArgumentCaptor<JobRunrMetadata> jobRunrMetadataArgumentCaptor;


    private BackgroundJobServerStatus backgroundJobServerStatus;
    private JobZooKeeper jobZooKeeper;
    private BackgroundJobTestFilter logAllStateChangesFilter;
    private ListAppender<ILoggingEvent> logger;

    @BeforeEach
    void setUpBackgroundJobZooKeeper() {
        logAllStateChangesFilter = new BackgroundJobTestFilter();
        backgroundJobServerStatus = aDefaultBackgroundJobServerStatus().withIsStarted().build();
        jobZooKeeper = initializeJobZooKeeper();
        logger = LoggerAssert.initFor(jobZooKeeper);
    }

    @Test
    void jobZooKeeperDoesNothingIfItIsNotInitialized() {
        when(backgroundJobServer.isUnAnnounced()).thenReturn(true);

        jobZooKeeper.run();

        verifyNoInteractions(storageProvider);
    }

    @Test
    void jobsThatAreProcessedAreBeingUpdatedWithAHeartbeat() {
        final Job job = anEnqueuedJob().withId().build();
        lenient().when(storageProvider.getJobs(eq(ENQUEUED), any())).thenReturn(singletonList(job));

        job.startProcessingOn(backgroundJobServer);
        jobZooKeeper.startProcessing(job, mock(Thread.class));
        jobZooKeeper.run();

        verify(storageProvider).save(singletonList(job));
        ProcessingState processingState = job.getJobState();
        assertThat(processingState.getUpdatedAt()).isAfter(processingState.getCreatedAt());
    }

    @Test
    void noExceptionIsThrownIfAJobHasSucceededWhileUpdateProcessingIsCalled() {
        // GIVEN
        final Job job = anEnqueuedJob().withId().build();
        job.startProcessingOn(backgroundJobServer);
        jobZooKeeper.startProcessing(job, mock(Thread.class));

        // WHEN
        job.succeeded();
        jobZooKeeper.run();

        // THEN
        assertThat(logger).hasNoWarnLogMessages();
        verify(storageProvider).save(singletonList(job));
    }

    @Test
    void evenWhenNoWorkCanBeOnboardedJobsThatAreProcessedAreBeingUpdatedWithAHeartbeat() {
        backgroundJobServerStatus = aDefaultBackgroundJobServerStatus().withWorkerSize(0).build();
        jobZooKeeper = initializeJobZooKeeper();

        final Job job = anEnqueuedJob().withId().build();
        lenient().when(storageProvider.getJobs(eq(ENQUEUED), any())).thenReturn(singletonList(job));

        job.startProcessingOn(backgroundJobServer);
        jobZooKeeper.startProcessing(job, mock(Thread.class));
        jobZooKeeper.run();
        jobZooKeeper.startProcessing(aJobInProgress().build(), mock(Thread.class));

        verify(storageProvider).save(singletonList(job));
        ProcessingState processingState = job.getJobState();
        assertThat(processingState.getUpdatedAt()).isAfter(processingState.getCreatedAt());
    }

    @Test
    void jobsThatAreBeingProcessedButHaveBeenDeletedViaDashboardWillBeInterrupted() {
        final Job job = anEnqueuedJob().withId().build();
        lenient().when(storageProvider.getJobs(eq(ENQUEUED), any())).thenReturn(singletonList(job));
        doThrow(new ConcurrentJobModificationException(job)).when(storageProvider).save(singletonList(job));
        when(storageProvider.getJobById(job.getId())).thenReturn(aCopyOf(job).withDeletedState().build());
        final Thread threadMock = mock(Thread.class);

        job.startProcessingOn(backgroundJobServer);
        jobZooKeeper.startProcessing(job, threadMock);
        jobZooKeeper.run();

        assertThat(logger).hasNoWarnLogMessages();

        assertThat(job).hasState(DELETED);
        verify(storageProvider).save(singletonList(job));
        verify(threadMock).interrupt();
    }

    @Test
    void checkForRecurringJobs() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/5 * * * * *").build();

        when(storageProvider.getRecurringJobs()).thenReturn(List.of(recurringJob));

        jobZooKeeper.run();

        verify(backgroundJobServer).scheduleJob(recurringJob);
    }

    @Test
    void checkForRecurringJobsDoesNotScheduleSameJobIfItIsAlreadyScheduledEnqueuedOrProcessed() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/5 * * * * *").build();

        when(storageProvider.getRecurringJobs()).thenReturn(List.of(recurringJob));
        when(storageProvider.recurringJobExists(recurringJob.getId(), SCHEDULED, ENQUEUED, PROCESSING)).thenReturn(true);

        jobZooKeeper.run();

        verify(backgroundJobServer, never()).scheduleJob(recurringJob);
    }

    @Test
    void checkForScheduledJobsEnqueuesJobsThatNeedToBeEnqueued() {
        final Job scheduledJob = aScheduledJob().build();
        final List<Job> jobs = List.of(scheduledJob);

        when(storageProvider.getScheduledJobs(any(), any())).thenReturn(jobs, emptyJobList());

        jobZooKeeper.run();

        verify(storageProvider).save(jobsToSaveArgumentCaptor.capture());
        assertThat(jobsToSaveArgumentCaptor.getValue().get(0)).hasStates(SCHEDULED, ENQUEUED);
    }

    @Test
    void checkForEnqueuedJobsIfJobsPresentSubmitsThemToTheBackgroundJobServer() {
        final Job enqueuedJob = anEnqueuedJob().build();
        final List<Job> jobs = List.of(enqueuedJob);

        lenient().when(storageProvider.getJobs(eq(SUCCEEDED), any(), any())).thenReturn(emptyList());
        lenient().when(storageProvider.getJobs(eq(ENQUEUED), any())).thenReturn(jobs);

        jobZooKeeper.run();

        verify(backgroundJobServer).processJob(enqueuedJob);
    }

    @Test
    void checkForEnqueuedJobsIsNotDoneConcurrently() throws InterruptedException {
        when(storageProvider.getJobs(eq(ENQUEUED), any())).thenAnswer((invocationOnMock) -> {
            sleep(100);
            return emptyList();
        });

        CountDownLatch countDownLatch = new CountDownLatch(2);
        final Thread thread1 = new Thread(() -> {
            jobZooKeeper.notifyThreadIdle();
            countDownLatch.countDown();
        });
        final Thread thread2 = new Thread(() -> {
            jobZooKeeper.notifyThreadIdle();
            countDownLatch.countDown();
        });
        thread1.start();
        thread2.start();

        countDownLatch.await();
        verify(storageProvider, times(1)).getJobs(eq(ENQUEUED), any());
    }

    @Test
    void checkForOrphanedJobs() {
        final Job orphanedJob = anEnqueuedJob().withState(new ProcessingState(backgroundJobServer.getId())).build();
        when(storageProvider.getJobs(eq(PROCESSING), any(Instant.class), any()))
                .thenReturn(
                        singletonList(orphanedJob),
                        emptyJobList()
                );

        jobZooKeeper.run();

        verify(storageProvider).save(jobsToSaveArgumentCaptor.capture());
        assertThat(jobsToSaveArgumentCaptor.getValue().get(0)).hasStates(ENQUEUED, PROCESSING, FAILED, SCHEDULED);
    }

    @Test
    void checkForSucceededJobsThanCanGoToDeletedState() {
        lenient().when(storageProvider.getJobs(eq(SUCCEEDED), any(Instant.class), any()))
                .thenReturn(
                        asList(aSucceededJob().build(), aSucceededJob().build(), aSucceededJob().build(), aSucceededJob().build(), aSucceededJob().build()),
                        emptyJobList()
                );

        jobZooKeeper.run();

        verify(storageProvider).save(anyList());
        verify(storageProvider).publishTotalAmountOfSucceededJobs(5);

        assertThat(logAllStateChangesFilter.stateChanges).containsExactly("SUCCEEDED->DELETED", "SUCCEEDED->DELETED", "SUCCEEDED->DELETED", "SUCCEEDED->DELETED", "SUCCEEDED->DELETED");
        assertThat(logAllStateChangesFilter.processingPassed).isFalse();
        assertThat(logAllStateChangesFilter.processedPassed).isFalse();
    }

    @Test
    void checkForSucceededJobsCanGoToDeletedStateAlsoWorksForInterfacesWithMethodsThatDontExistAnymore() {
        // GIVEN
        lenient().when(storageProvider.getJobs(eq(SUCCEEDED), any(Instant.class), any()))
                .thenReturn(
                        asList(aSucceededJob()
                                .withJobDetails(jobDetails()
                                        .withClassName(TestServiceInterface.class)
                                        .withMethodName("methodThatDoesNotExist")
                                        .build())
                                .build()),
                        emptyJobList()
                );

        // WHEN
        jobZooKeeper.run();

        // THEN
        assertThat(logger).hasNoWarnLogMessages();

        verify(storageProvider).save(anyList());
        verify(storageProvider).publishTotalAmountOfSucceededJobs(1);
    }

    @Test
    void checkForJobsThatCanBeDeleted() {
        when(storageProvider.deleteJobsPermanently(eq(DELETED), any())).thenReturn(5);

        jobZooKeeper.run();

        verify(storageProvider).deleteJobsPermanently(eq(DELETED), any());
    }

    @Test
    void allStateChangesArePassingViaTheApplyStateFilterOnSuccess() {
        Job job = aScheduledJob().build();

        when(storageProvider.getScheduledJobs(any(Instant.class), any()))
                .thenReturn(
                        singletonList(job),
                        emptyJobList()
                );

        jobZooKeeper.run();

        assertThat(logAllStateChangesFilter.stateChanges).containsExactly("SCHEDULED->ENQUEUED");
        assertThat(logAllStateChangesFilter.processingPassed).isFalse();
        assertThat(logAllStateChangesFilter.processedPassed).isFalse();
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/27")
    void jobNotFoundExceptionsDoNotCauseTheBackgroundJobServerToStop() {
        Job job = aSucceededJob().withJobDetails(methodThatDoesNotExistJobDetails()).build();

        lenient().when(storageProvider.getJobs(eq(SUCCEEDED), any(Instant.class), any()))
                .thenReturn(
                        singletonList(job),
                        emptyJobList());

        jobZooKeeper.run();

        AtomicInteger exceptionCount = Whitebox.getInternalState(jobZooKeeper, "exceptionCount");
        assertThat(exceptionCount).hasValue(0);
        assertThat(logger).hasNoWarnLogMessages();
    }

    @Test
    void severeJobRunrExceptionsAreLoggedToStorageProvider() {
        Job succeededJob1 = aSucceededJob().build();
        Job succeededJob2 = aSucceededJob().build();

        when(storageProvider.getJobById(succeededJob1.getId())).thenReturn(succeededJob1);
        when(storageProvider.getJobById(succeededJob2.getId())).thenReturn(succeededJob2);
        lenient().when(storageProvider.getJobs(eq(SUCCEEDED), any(Instant.class), any()))
                .thenReturn(
                        asList(succeededJob1, succeededJob2, aSucceededJob().build(), aSucceededJob().build(), aSucceededJob().build()),
                        emptyJobList()
                );
        when(storageProvider.save(anyList())).thenThrow(new ConcurrentJobModificationException(asList(succeededJob1, succeededJob2)));

        jobZooKeeper.run();

        verify(storageProvider).saveMetadata(jobRunrMetadataArgumentCaptor.capture());

        assertThat(jobRunrMetadataArgumentCaptor.getValue())
                .hasName(SevereJobRunrException.class.getSimpleName())
                .hasOwner("BackgroundJobServer " + backgroundJobServer.getId())
                .valueContains("## Runtime information");
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/122")
    void masterTasksArePostponedToNextRunIfPollIntervalInSecondsTimeboxIsAboutToPass() {
        when(backgroundJobServer.isUnAnnounced()).then(putRunStartTimeInPast());

        jobZooKeeper.run();

        verify(storageProvider, never()).getScheduledJobs(any(Instant.class), any(PageRequest.class));
        verify(storageProvider, never()).getJobs(eq(PROCESSING), any(Instant.class), any(PageRequest.class));
        verify(storageProvider, never()).getJobs(eq(SUCCEEDED), any(Instant.class), any(PageRequest.class));
    }

    private JobZooKeeper initializeJobZooKeeper() {
        UUID backgroundJobServerId = UUID.randomUUID();
        lenient().when(backgroundJobServer.getId()).thenReturn(backgroundJobServerId);
        when(backgroundJobServer.getStorageProvider()).thenReturn(storageProvider);
        when(backgroundJobServer.getServerStatus()).thenReturn(backgroundJobServerStatus);
        when(backgroundJobServer.getWorkDistributionStrategy()).thenReturn(workDistributionStrategy);
        when(backgroundJobServer.getJobFilters()).thenReturn(new JobDefaultFilters(logAllStateChangesFilter));
        when(backgroundJobServer.getDashboardNotificationManager()).thenReturn(new DashboardNotificationManager(backgroundJobServerId, storageProvider));
        lenient().when(workDistributionStrategy.canOnboardNewWork()).thenReturn(true);
        lenient().when(workDistributionStrategy.getWorkPageRequest()).thenReturn(ascOnUpdatedAt(10));
        lenient().when(backgroundJobServer.isAnnounced()).thenReturn(true);
        lenient().when(backgroundJobServer.isMaster()).thenReturn(true);
        return new JobZooKeeper(backgroundJobServer);
    }

    private List<Job>[] emptyJobList() {
        List<Job>[] result = cast(new ArrayList[1]);
        result[0] = new ArrayList<>();
        return result;
    }

    private Answer<Boolean> putRunStartTimeInPast() {
        return invocation -> {
            Whitebox.setInternalState(jobZooKeeper, "runStartTime", System.currentTimeMillis() - 15000);
            return false;
        };
    }
}