package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.SevereJobRunrException;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobFilterUtils;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolver;
import org.jobrunr.server.concurrent.UnresolvableConcurrentJobModificationException;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.jobrunr.utils.streams.StreamUtils.consumerToFunction;

public abstract class ZooKeeperTask {

    protected static final Logger LOGGER = LoggerFactory.getLogger(JobZooKeeper.class);

    protected final JobZooKeeper jobZooKeeper;
    protected final BackgroundJobServer backgroundJobServer;
    protected final StorageProvider storageProvider;
    protected final JobFilterUtils jobFilterUtils;
    private final ConcurrentJobModificationResolver concurrentJobModificationResolver;

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

    protected final void processManyJobs(Function<List<Job>, List<Job>> jobListSupplier, Consumer<Job> jobConsumer, Consumer<Integer> amountOfProcessedJobsConsumer) {
        convertAndProcessManyJobs(jobListSupplier, consumerToFunction(jobConsumer), amountOfProcessedJobsConsumer);
    }

    protected final <T> void convertAndProcessManyJobs(Function<List<T>, List<T>> itemSupplier, Function<T, Job> toJobFunction, Consumer<Integer> amountOfProcessedJobsConsumer) {
        int amountOfProcessedJobs = 0;
        List<T> items = getItemsToProcess(itemSupplier, null);
        while (!items.isEmpty()) {
            processJobs(items, toJobFunction);
            amountOfProcessedJobs += items.size();
            items = getItemsToProcess(itemSupplier, items);
        }
        amountOfProcessedJobsConsumer.accept(amountOfProcessedJobs);
    }

    protected final <T> List<Job> convertAndProcessManyJobs(List<T> items, Function<T, List<Job>> toJobsFunction, Consumer<Integer> amountOfProcessedJobsConsumer) {
        List<Job> jobs = items.stream().map(toJobsFunction).flatMap(List::stream).filter(Objects::nonNull).collect(toList());
        saveAndRunJobFilters(jobs);
        amountOfProcessedJobsConsumer.accept(jobs.size());
        return jobs;
    }

    protected final <T> List<Job> processJobs(List<T> items, Function<T, Job> toJobFunction) {
        List<Job> jobs = items.stream().map(toJobFunction).filter(Objects::nonNull).collect(toList());
        return saveAndRunJobFilters(jobs);
    }

    protected List<Job> saveAndRunJobFilters(List<Job> jobs) {
        if (jobs.isEmpty()) return emptyList();

        try {
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
        return jobs;
    }

    protected <T> List<T> getItemsToProcess(Function<List<T>, List<T>> jobListSupplier, List<T> previousItemsToProcess) {
        if (pollIntervalInSecondsTimeBoxIsAboutToPass()) return emptyList();
        return jobListSupplier.apply(previousItemsToProcess);
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

    BackgroundJobServerConfiguration backgroundJobServerConfiguration() {
        return runInfo.getBackgroundJobServerConfiguration();
    }

    Instant runStartTime() {
        return runInfo.getRunStartTime();
    }
}
