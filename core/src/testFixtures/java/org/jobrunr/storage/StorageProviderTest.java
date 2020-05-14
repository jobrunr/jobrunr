package org.jobrunr.storage;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.scheduling.cron.CronExpression;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.ServerZooKeeper;
import org.jobrunr.stubs.BackgroundJobServerStub;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.streams.StreamUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.jobrunr.jobs.JobDetailsTestBuilder.defaultJobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.systemOutPrintLnJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJob;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.JobTestBuilder.aScheduledJob;
import static org.jobrunr.jobs.JobTestBuilder.aSucceededJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;

public abstract class StorageProviderTest {

    private StorageProvider storageProvider;

    private BackgroundJobServer backgroundJobServer;

    @BeforeEach
    public void cleanUpAndSetupBackgroundJobServer() {
        cleanup();
        final BackgroundJobServerStatus serverStatus = new ServerZooKeeper.BackgroundJobServerStatusWriteModel(new BackgroundJobServerStatus(15, 10));
        serverStatus.start();
        JobRunr.configure();
        this.storageProvider = getStorageProvider();
        backgroundJobServer = new BackgroundJobServerStub(storageProvider, serverStatus);
    }

    protected abstract void cleanup();

    protected abstract StorageProvider getStorageProvider();

    @Test
    public void testAnnounceAndListBackgroundJobServers() throws InterruptedException {
        final BackgroundJobServerStatus serverStatus1 = new ServerZooKeeper.BackgroundJobServerStatusWriteModel(new BackgroundJobServerStatus(15, 10));
        serverStatus1.start();
        storageProvider.announceBackgroundJobServer(serverStatus1);
        Thread.sleep(100);

        final BackgroundJobServerStatus serverStatus2 = new ServerZooKeeper.BackgroundJobServerStatusWriteModel(new BackgroundJobServerStatus(15, 10));
        serverStatus2.start();
        storageProvider.announceBackgroundJobServer(serverStatus2);
        Thread.sleep(100);

        storageProvider.signalBackgroundJobServerAlive(serverStatus1);
        storageProvider.signalBackgroundJobServerAlive(serverStatus2);

        final List<BackgroundJobServerStatus> backgroundJobServers = storageProvider.getBackgroundJobServers();

        assertThat(backgroundJobServers).hasSize(2);
        //why: sqlite has no microseconds precision for timestamps
        assertThat(backgroundJobServers.get(0)).isEqualToComparingOnlyGivenFields(serverStatus1, "id", "workerPoolSize", "pollIntervalInSeconds", "running");
        assertThat(backgroundJobServers.get(1)).isEqualToComparingOnlyGivenFields(serverStatus2, "id", "workerPoolSize", "pollIntervalInSeconds", "running");
        assertThat(backgroundJobServers.get(0).getFirstHeartbeat()).isCloseTo(serverStatus1.getFirstHeartbeat(), within(1000, ChronoUnit.MICROS));
        assertThat(backgroundJobServers.get(0).getLastHeartbeat()).isAfter(backgroundJobServers.get(0).getFirstHeartbeat());
        assertThat(backgroundJobServers.get(1).getFirstHeartbeat()).isCloseTo(serverStatus2.getFirstHeartbeat(), within(1000, ChronoUnit.MICROS));
        assertThat(backgroundJobServers.get(1).getLastHeartbeat()).isAfter(backgroundJobServers.get(1).getFirstHeartbeat());
    }

    @Test
    public void testRemoveTimedOutBackgroundJobServers() throws InterruptedException {
        final BackgroundJobServerStatus serverStatus1 = new ServerZooKeeper.BackgroundJobServerStatusWriteModel(new BackgroundJobServerStatus(15, 10));
        serverStatus1.start();
        storageProvider.announceBackgroundJobServer(serverStatus1);
        Thread.sleep(50);
        Instant deleteServersWithHeartbeatOlderThanThis = now();
        Thread.sleep(50);

        final BackgroundJobServerStatus serverStatus2 = new ServerZooKeeper.BackgroundJobServerStatusWriteModel(new BackgroundJobServerStatus(15, 10));
        serverStatus2.start();
        storageProvider.announceBackgroundJobServer(serverStatus2);

        final int deletedServers = storageProvider.removeTimedOutBackgroundJobServers(deleteServersWithHeartbeatOlderThanThis);
        assertThat(deletedServers).isEqualTo(1);
        assertThat(storageProvider.getBackgroundJobServers()).hasSize(1);
        assertThat(storageProvider.getBackgroundJobServers().get(0).getId()).isEqualTo(serverStatus2.getId());
    }

    @Test
    public void ifServerHasTimedOutAndSignalsItsAliveAnExceptionIsThrown() throws InterruptedException {
        final BackgroundJobServerStatus serverStatus = new ServerZooKeeper.BackgroundJobServerStatusWriteModel(new BackgroundJobServerStatus(15, 10));
        serverStatus.start();
        storageProvider.announceBackgroundJobServer(serverStatus);
        Thread.sleep(100);

        Instant deleteServersWithHeartbeatOlderThanThis = now();
        storageProvider.removeTimedOutBackgroundJobServers(deleteServersWithHeartbeatOlderThanThis);

        assertThatThrownBy(() -> storageProvider.signalBackgroundJobServerAlive(serverStatus)).isInstanceOf(ServerTimedOutException.class);
    }

    @Test
    public void testCRUDJob() {
        Job job = anEnqueuedJob().build();
        Job createdJob = storageProvider.save(job);
        Job fetchedJob = storageProvider.getJobById(createdJob.getId());
        assertThat(fetchedJob).usingRecursiveComparison().isEqualTo(createdJob);

        fetchedJob.startProcessingOn(backgroundJobServer);
        storageProvider.save(fetchedJob);

        Job fetchedUpdatedJob = storageProvider.getJobById(createdJob.getId());
        assertThat(fetchedUpdatedJob).usingRecursiveComparison().isEqualTo(fetchedJob);

        final int deletedJobs = storageProvider.delete(fetchedJob.getId());
        assertThat(deletedJobs).isEqualTo(1);
        assertThatThrownBy(() -> storageProvider.getJobById(fetchedJob.getId())).isInstanceOf(JobNotFoundException.class);
    }

    @Test
    public void testOptimisticLocking() {
        Job job = anEnqueuedJob().build();
        Job createdJob = storageProvider.save(job);
        Job fetchedJob = storageProvider.getJobById(createdJob.getId());

        JobMapper jobMapper = new JobMapper(new JacksonJsonMapper());
        Job job1 = jobMapper.deserializeJob(jobMapper.serializeJob(fetchedJob));
        Job job2 = jobMapper.deserializeJob(jobMapper.serializeJob(fetchedJob));

        job1.startProcessingOn(backgroundJobServer);
        job2.startProcessingOn(backgroundJobServer);

        storageProvider.save(job1);
        assertThatThrownBy(() -> storageProvider.save(job2)).isInstanceOf(ConcurrentJobModificationException.class);
    }

    @Test
    public void testUpdateAndGet() {
        Job job = anEnqueuedJob().build();
        Job createdJob = storageProvider.save(job);
        createdJob.startProcessingOn(backgroundJobServer);
        Job processingJob = storageProvider.save(createdJob);
        processingJob.succeeded();
        Job succeededJob = storageProvider.save(processingJob);

        Job fetchedJob = storageProvider.getJobById(createdJob.getId());
        assertThat(fetchedJob).usingRecursiveComparison().isEqualTo(succeededJob);
    }

    @Test
    public void testExists() {
        JobDetails jobDetails = defaultJobDetails().build();
        Job job = anEnqueuedJob()
                .withJobDetails(jobDetails)
                .build();
        storageProvider.save(job);

        assertThat(storageProvider.exists(jobDetails, ENQUEUED)).isTrue();
        assertThat(storageProvider.exists(jobDetails, SUCCEEDED)).isFalse();
    }

    @Test
    public void testSaveListUpdateListAndGetListOfJobs() {
        final List<Job> jobs = asList(
                aJob().withName("1").withEnqueuedState(now().minusSeconds(30)).build(),
                aJob().withName("2").withEnqueuedState(now().minusSeconds(20)).build(),
                aJob().withName("3").withEnqueuedState(now().minusSeconds(10)).build());
        final List<Job> savedJobs = storageProvider.save(jobs);

        savedJobs.forEach(job -> job.startProcessingOn(backgroundJobServer));
        storageProvider.save(savedJobs);

        assertThat(storageProvider.countJobs(ENQUEUED)).isEqualTo(0);
        assertThat(storageProvider.countJobs(PROCESSING)).isEqualTo(3);

        List<Job> fetchedJobsAsc = storageProvider.getJobs(PROCESSING, PageRequest.asc(0, 100));
        assertThat(fetchedJobsAsc)
                .hasSize(3)
                .usingRecursiveFieldByFieldElementComparator().containsAll(savedJobs);
        assertThat(fetchedJobsAsc).extracting("jobName").containsExactly("1", "2", "3");

        List<Job> fetchedJobsDesc = storageProvider.getJobs(PROCESSING, PageRequest.desc(0, 100));
        assertThat(fetchedJobsDesc)
                .hasSize(3)
                .usingRecursiveFieldByFieldElementComparator().containsAll(savedJobs);
        assertThat(fetchedJobsDesc).extracting("jobName").containsExactly("3", "2", "1");
    }

    @Test
    public void testGetListOfJobsUpdatedBefore() {
        final List<Job> jobs = asList(
                aJob().withEnqueuedState(now().minus(24, HOURS)).build(),
                aJob().withEnqueuedState(now().minus(12, HOURS)).build(),
                aJob().withEnqueuedState(now().minus(1, HOURS)).build(),
                aJob().withEnqueuedState(now()).build()
        );
        storageProvider.save(jobs);

        assertThat(storageProvider.getJobs(ENQUEUED, now().minus(2, HOURS), PageRequest.asc(0, 100))).hasSize(2);
        assertThat(storageProvider.getJobs(ENQUEUED, now().minus(1, HOURS), PageRequest.asc(0, 100))).hasSize(3);
    }

    @Test
    public void testDeleteJobs() {
        final List<Job> jobs = asList(
                aJob().withEnqueuedState(now().minus(4, HOURS)).build(),
                aJob().withEnqueuedState(now().minus(3, HOURS)).build(),
                aJob().withEnqueuedState(now().minus(2, HOURS)).build(),
                aJob().withEnqueuedState(now()).build()
        );

        storageProvider.save(jobs);

        storageProvider.deleteJobs(ENQUEUED, now().minus(1, HOURS));

        List<Job> fetchedJobs = storageProvider.getJobs(ENQUEUED, PageRequest.asc(0, 100));

        assertThat(fetchedJobs).hasSize(1);
    }

    @Test
    public void testSaveListAndPageWithOffsetAndLimit() {
        Job job1 = aJob().withEnqueuedState(now().minusSeconds(10)).build();
        Job job2 = aJob().withEnqueuedState(now().minusSeconds(8)).build();
        Job job3 = aJob().withEnqueuedState(now().minusSeconds(6)).build();
        Job job4 = aJob().withEnqueuedState(now().minusSeconds(4)).build();
        Job job5 = aJob().withEnqueuedState(now().minusSeconds(2)).build();
        final List<Job> jobs = asList(job1, job2, job3, job4, job5);

        storageProvider.save(jobs);

        Page<Job> fetchedJobs = storageProvider.getJobPage(ENQUEUED, PageRequest.asc(2, 2));

        assertThat(fetchedJobs.getItems())
                .hasSize(2)
                .usingRecursiveFieldByFieldElementComparator().containsExactly(job3, job4);
    }

    @Test
    public void testScheduledJobs() {
        Job job1 = anEnqueuedJob().withState(new ScheduledState(now())).build();
        Job job2 = anEnqueuedJob().withState(new ScheduledState(now().plus(20, HOURS))).build();
        final List<Job> jobs = asList(job1, job2);

        storageProvider.save(jobs);

        assertThat(storageProvider.getScheduledJobs(now().plus(5, ChronoUnit.SECONDS), PageRequest.asc(0, 100))).hasSize(1);
    }

    @Test
    public void testCRUDRecurringJob() {
        RecurringJob recurringJobv1 = new RecurringJob("my-job", defaultJobDetails().build(), CronExpression.create(Cron.daily()), ZoneId.systemDefault());
        storageProvider.saveRecurringJob(recurringJobv1);
        assertThat(storageProvider.getRecurringJobs()).hasSize(1);

        RecurringJob recurringJobv2 = new RecurringJob("my-job", defaultJobDetails().build(), CronExpression.create(Cron.hourly()), ZoneId.systemDefault());
        storageProvider.saveRecurringJob(recurringJobv2);
        assertThat(storageProvider.getRecurringJobs()).hasSize(1);
        assertThat(storageProvider.getRecurringJobs().get(0).getCronExpression()).isEqualTo(Cron.hourly());

        storageProvider.deleteRecurringJob("my-job");

        assertThat(storageProvider.getRecurringJobs()).hasSize(0);
    }

    @Test
    public void testOnChangeListenerForSaveJob() {
        final SimpleJobStorageOnChangeListener onChangeListener = new SimpleJobStorageOnChangeListener();
        storageProvider.addJobStorageOnChangeListener(onChangeListener);

        storageProvider.save(anEnqueuedJob().build());

        assertThat(onChangeListener.changes).hasSize(1);
    }

    @Test
    public void testOnChangeListenerForDeleteJob() {
        final Job job = anEnqueuedJob().build();
        storageProvider.save(job);

        final SimpleJobStorageOnChangeListener onChangeListener = new SimpleJobStorageOnChangeListener();
        storageProvider.addJobStorageOnChangeListener(onChangeListener);

        storageProvider.delete(job.getId());

        assertThat(onChangeListener.changes).hasSize(1);
    }

    @Test
    public void testOnChangeListenerForSaveJobList() {
        final SimpleJobStorageOnChangeListener onChangeListener = new SimpleJobStorageOnChangeListener();
        storageProvider.addJobStorageOnChangeListener(onChangeListener);

        final List<Job> jobs = asList(anEnqueuedJob().build(), anEnqueuedJob().build());
        storageProvider.save(jobs);
        assertThat(onChangeListener.changes).hasSize(1);

        jobs.forEach(job -> job.startProcessingOn(backgroundJobServer));
        storageProvider.save(jobs);
        assertThat(onChangeListener.changes).hasSize(2);
    }

    @Test
    public void testOnChangeListenerForDeleteJobsByState() {
        storageProvider.save(asList(anEnqueuedJob().build(), anEnqueuedJob().build()));

        final SimpleJobStorageOnChangeListener onChangeListener = new SimpleJobStorageOnChangeListener();
        storageProvider.addJobStorageOnChangeListener(onChangeListener);

        storageProvider.deleteJobs(ENQUEUED, now());

        assertThat(onChangeListener.changes).hasSize(1);
    }

    @Test
    public void testJobStats() {
        storageProvider.announceBackgroundJobServer(backgroundJobServer.getServerStatus());

        assertThatCode(() -> storageProvider.getJobStats()).doesNotThrowAnyException();

        storageProvider.save(asList(
                anEnqueuedJob().build(),
                anEnqueuedJob().build(),
                anEnqueuedJob().build(),
                aScheduledJob().withoutId().build(),
                aFailedJob().withoutId().build(),
                aFailedJob().withoutId().build(),
                aSucceededJob().withoutId().build()
        ));
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("id1").build());
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("id2").build());
        storageProvider.publishJobStatCounter(SUCCEEDED, 5);

        final JobStats jobStats = storageProvider.getJobStats();
        assertThat(jobStats.getAwaiting()).isEqualTo(0);
        assertThat(jobStats.getScheduled()).isEqualTo(1);
        assertThat(jobStats.getEnqueued()).isEqualTo(3);
        assertThat(jobStats.getProcessing()).isEqualTo(0);
        assertThat(jobStats.getFailed()).isEqualTo(2);
        assertThat(jobStats.getSucceeded()).isEqualTo(6);
        assertThat(jobStats.getRecurringJobs()).isEqualTo(2);
        assertThat(jobStats.getBackgroundJobServers()).isEqualTo(1);
    }

    @Test
    @Disabled
    public void testPerformance() {
        int amount = 1000000;
        IntStream.range(0, amount)
                .peek(i -> {
                    if (i % 10000 == 0) {
                        System.out.println("Saving job " + i);
                    }
                })
                .mapToObj(i -> anEnqueuedJob().withJobDetails(systemOutPrintLnJobDetails("this is test " + i)).build())
                .collect(StreamUtils.batchCollector(1000, storageProvider::save));

        AtomicInteger atomicInteger = new AtomicInteger();
        storageProvider
                .getJobs(ENQUEUED, PageRequest.asc(0, 1000))
                .stream()
                .peek(job -> {
                    if (atomicInteger.get() % 10000 == 0) {
                        System.out.println("Retrieved job " + atomicInteger.get());
                    }
                })
                .forEach(job -> atomicInteger.incrementAndGet());

        assertThat(atomicInteger).hasValue(amount);
    }

    private static class SimpleJobStorageOnChangeListener implements JobStorageChangeListener {

        private List<JobStats> changes = new ArrayList<>();

        @Override
        public void onChange(JobStats jobStats) {
            this.changes.add(jobStats);
        }
    }
}
