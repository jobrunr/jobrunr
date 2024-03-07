package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.tasks.Task;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static org.jobrunr.utils.streams.StreamUtils.consumerToFunction;

public abstract class JobZooKeeperTask extends Task {

    protected JobZooKeeperTask(BackgroundJobServer backgroundJobServer) {
        super(backgroundJobServer);
    }

    protected final void processManyJobs(Function<List<Job>, List<Job>> jobListSupplier, Consumer<Job> jobConsumer, Consumer<Integer> amountOfProcessedJobsConsumer) {
        convertAndProcessManyJobs(jobListSupplier, consumerToFunction(jobConsumer), amountOfProcessedJobsConsumer);
    }

    protected final <T> void convertAndProcessManyJobs(Function<List<T>, List<T>> itemSupplier, Function<T, Job> toJobFunction, Consumer<Integer> amountOfProcessedJobsConsumer) {
        int amountOfProcessedJobs = 0;
        List<T> items = getItemsToProcess(itemSupplier, null);
        while (!items.isEmpty()) {
            convertAndProcessJobs(items, toJobFunction);
            amountOfProcessedJobs += items.size();
            items = getItemsToProcess(itemSupplier, items);
        }
        amountOfProcessedJobsConsumer.accept(amountOfProcessedJobs);
    }

    protected final <T> void convertAndProcessManyJobs(List<T> items, Function<T, List<Job>> toJobsFunction, Consumer<Integer> amountOfProcessedJobsConsumer) {
        List<Job> jobs = items.stream().map(toJobsFunction).flatMap(List::stream).filter(Objects::nonNull).collect(toList());
        saveAndRunJobFilters(jobs);
        amountOfProcessedJobsConsumer.accept(jobs.size());
    }
}
