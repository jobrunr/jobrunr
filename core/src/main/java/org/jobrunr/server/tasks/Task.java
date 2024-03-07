package org.jobrunr.server.tasks;

import org.jobrunr.SevereJobRunrException;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobFilterUtils;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfigurationReader;
import org.jobrunr.server.concurrent.UnresolvableConcurrentJobModificationException;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

public abstract class Task {

    protected final Logger LOGGER;

    protected final BackgroundJobServer backgroundJobServer;
    protected final StorageProvider storageProvider;
    protected final JobFilterUtils jobFilterUtils;
    protected TaskRunInfo runInfo;

    protected Task(BackgroundJobServer backgroundJobServer) {
        this.LOGGER = LoggerFactory.getLogger(this.getClass());
        this.backgroundJobServer = backgroundJobServer;
        this.storageProvider = backgroundJobServer.getStorageProvider();
        this.jobFilterUtils = new JobFilterUtils(backgroundJobServer.getJobFilters());
    }

    public void run(TaskRunInfo runInfo) {
        // placeholder for license guard
        try {
            this.runInfo = runInfo;
            if (pollIntervalInSecondsTimeBoxIsAboutToPass()) return;
            long startTime = System.nanoTime();
            runTask();
            long endTime = System.nanoTime();
            LOGGER.trace("task took {}.", Duration.ofNanos((endTime - startTime)));
        } finally {
            this.runInfo = null;
        }
    }

    protected abstract void runTask();

    protected final <T> void convertAndProcessJobs(List<T> items, Function<T, Job> toJobFunction) {
        List<Job> jobs = items.stream().map(toJobFunction).filter(Objects::nonNull).collect(toList());
        saveAndRunJobFilters(jobs);
    }

    protected void saveAndRunJobFilters(List<Job> jobs) {
        if (jobs.isEmpty()) return;

        try {
            jobFilterUtils.runOnStateElectionFilter(jobs);
            storageProvider.save(jobs);
            jobFilterUtils.runOnStateAppliedFilters(jobs);
        } catch (ConcurrentJobModificationException concurrentJobModificationException) {
            try {
                backgroundJobServer.getConcurrentJobModificationResolver().resolve(concurrentJobModificationException);
            } catch (UnresolvableConcurrentJobModificationException unresolvableConcurrentJobModificationException) {
                throw new SevereJobRunrException("Could not resolve ConcurrentJobModificationException", unresolvableConcurrentJobModificationException);
            }
        }
    }

    protected BackgroundJobServerConfigurationReader backgroundJobServerConfiguration() {
        return runInfo.getBackgroundJobServerConfiguration();
    }

    protected Instant runStartTime() {
        return runInfo.getRunStartTime();
    }

    protected boolean pollIntervalInSecondsTimeBoxIsAboutToPass() {
        return runInfo.pollIntervalInSecondsTimeBoxIsAboutToPass();
    }
}
