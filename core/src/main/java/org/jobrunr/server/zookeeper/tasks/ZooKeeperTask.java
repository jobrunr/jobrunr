package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.SevereJobRunrException;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobFilterUtils;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolver;
import org.jobrunr.server.concurrent.UnresolvableConcurrentJobModificationException;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;

public abstract class ZooKeeperTask {

    protected static final Logger LOGGER = LoggerFactory.getLogger(JobZooKeeper.class);
    private final JobFilterUtils jobFilterUtils;
    private final ConcurrentJobModificationResolver concurrentJobModificationResolver;

    protected final JobZooKeeper jobZooKeeper;
    protected final BackgroundJobServer backgroundJobServer;
    protected final StorageProvider storageProvider;
    protected ZooKeeperTaskInfo runInfo;

    protected ZooKeeperTask(JobZooKeeper jobZooKeeper, BackgroundJobServer backgroundJobServer) {
        this.jobZooKeeper = jobZooKeeper;
        this.backgroundJobServer = backgroundJobServer;
        this.storageProvider = backgroundJobServer.getStorageProvider();
        this.jobFilterUtils = new JobFilterUtils(backgroundJobServer.getJobFilters());
        this.concurrentJobModificationResolver = createConcurrentJobModificationResolver();
    }

    public void run(ZooKeeperTaskInfo runInfo) {
        try {
            this.runInfo = runInfo;
            if (pollIntervalInSecondsTimeBoxIsAboutToPass()) return;
            runTask();
        } finally {
            this.runInfo = null;
        }
    }

    protected abstract void runTask();

    protected void processJobList(Supplier<List<Job>> jobListSupplier, Consumer<Job> jobConsumer, Consumer<Integer> amountOfProcessedJobsConsumer) {
        int amountOfProcessedJobs = 0;
        List<Job> jobs = getJobsToProcess(jobListSupplier);
        while (!jobs.isEmpty()) {
            processJobList(jobs, jobConsumer);
            amountOfProcessedJobs += jobs.size();
            jobs = getJobsToProcess(jobListSupplier);
        }
        amountOfProcessedJobsConsumer.accept(amountOfProcessedJobs);
    }

    protected void processJobList(List<Job> jobs, Consumer<Job> jobConsumer) {
        if (!jobs.isEmpty()) {
            try {
                jobs.forEach(jobConsumer);
                jobFilterUtils.runOnStateElectionFilter(jobs);
                storageProvider.save(jobs);
                jobFilterUtils.runOnStateAppliedFilters(jobs);
            } catch (ConcurrentJobModificationException concurrentJobModificationException) {
                try {
                    concurrentJobModificationResolver.resolve(concurrentJobModificationException);
                } catch (UnresolvableConcurrentJobModificationException unresolvableConcurrentJobModificationException) {
                    throw new SevereJobRunrException("Could not resolve ConcurrentJobModificationException", unresolvableConcurrentJobModificationException);
                }
            }
        }
    }

    private List<Job> getJobsToProcess(Supplier<List<Job>> jobListSupplier) {
        if (pollIntervalInSecondsTimeBoxIsAboutToPass()) return emptyList();
        return jobListSupplier.get();
    }

    protected BackgroundJobServerConfiguration configuration() {
        return backgroundJobServer.getConfiguration();
    }

    private boolean pollIntervalInSecondsTimeBoxIsAboutToPass() {
        return runInfo.pollIntervalInSecondsTimeBoxIsAboutToPass();
    }

    ConcurrentJobModificationResolver createConcurrentJobModificationResolver() {
        return backgroundJobServer.getConfiguration()
                .getConcurrentJobModificationPolicy()
                .toConcurrentJobModificationResolver(storageProvider, jobZooKeeper);
    }

    BackgroundJobServerStatus serverStatus() {
        return runInfo.getBackgroundJobServerStatus();
    }

    Instant runStartTime() {
        return runInfo.getRunStartTime();
    }
}
