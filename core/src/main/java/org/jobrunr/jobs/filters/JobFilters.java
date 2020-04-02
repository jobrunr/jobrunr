package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.jobrunr.jobs.filters.JobFilterUtils.initJobFilters;

public class JobFilters {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobFilters.class);

    private final List<JobFilter> filters;

    public JobFilters(JobFilter... filters) {
        this(Arrays.asList(filters));
    }

    public JobFilters(List<JobFilter> filters) {
        this.filters = getAllJobFilters(filters);
    }

    private ArrayList<JobFilter> getAllJobFilters(List<JobFilter> jobFilters) {
        final ArrayList<JobFilter> result = new ArrayList<>(Arrays.asList(new DisplayNameFilter(), new RetryFilter()));
        result.addAll(jobFilters);
        return result;
    }

    public void runOnCreatingFilter(List<Job> jobs) {
        jobs.forEach(this::runOnCreatingFilter);
    }

    public void runOnCreatedFilter(List<Job> jobs) {
        jobs.forEach(this::runOnCreatedFilter);
    }

    public void runOnCreatingFilter(AbstractJob job) {
        jobClientFilters(job).forEach(catchThrowable(jobClientFilter -> jobClientFilter.onCreating(job)));
    }

    public void runOnCreatedFilter(AbstractJob job) {
        jobClientFilters(job).forEach(catchThrowable(jobClientFilter -> jobClientFilter.onCreated(job)));
    }

    public void runOnStateElectionFilter(List<Job> jobs) {
        jobs.forEach(this::runOnStateElectionFilter);
    }

    public void runOnStateAppliedFilters(List<Job> jobs) {
        jobs.forEach(this::runOnStateAppliedFilters);
    }

    public void runOnStateElectionFilter(Job job) {
        electStateFilters(job).forEach(catchThrowable(electStateFilter -> electStateFilter.onStateElection(job, job.getJobState())));
    }

    public void runOnStateAppliedFilters(Job job) {
        applyStateFilters(job).forEach(catchThrowable(applyStateFilter -> applyStateFilter.onStateApplied(job, job.getJobState(-2), job.getJobState(-1))));
    }

    public void runOnJobProcessingFilters(Job job) {
        jobServerFilters(job).forEach(catchThrowable(jobServerFilter -> jobServerFilter.onProcessing(job)));
    }

    public void runOnJobProcessedFilters(Job job) {
        jobServerFilters(job).forEach(catchThrowable(jobServerFilter -> jobServerFilter.onProcessed(job)));
    }

    private Stream<ElectStateFilter> electStateFilters(Job job) {
        return JobFilterUtils.electStateFilters(initJobFilters(job, initJobFilters(job, filters)));
    }

    private Stream<ApplyStateFilter> applyStateFilters(Job job) {
        return JobFilterUtils.applyStateFilters(initJobFilters(job, initJobFilters(job, filters)));
    }

    private Stream<JobServerFilter> jobServerFilters(Job job) {
        return JobFilterUtils.jobServerFilters(initJobFilters(job, initJobFilters(job, filters)));
    }

    private Stream<JobClientFilter> jobClientFilters(AbstractJob job) {
        return JobFilterUtils.jobClientFilters(initJobFilters(job, initJobFilters(job, filters)));
    }

    static <T extends JobFilter> Consumer<T> catchThrowable(Consumer<T> consumer) {
        return jobClientFilter -> {
            try {
                consumer.accept(jobClientFilter);
            } catch (Exception e) {
                LOGGER.error("Error evaluating jobfilter {}", jobClientFilter.getClass().getName(), e);
            }
        };
    }
}
