package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.AbstractJob;
import org.jobrunr.jobs.Job;

import java.util.List;

public class JobFilterUtils {

    private JobDefaultFilters jobDefaultFilters;

    public JobFilterUtils(JobDefaultFilters jobDefaultFilters) {
        this.jobDefaultFilters = jobDefaultFilters;
    }

    public void runOnCreatingFilter(AbstractJob job) {
        new JobCreationFilters(job, jobDefaultFilters).runOnCreatingFilter();
    }

    public void runOnCreatedFilter(AbstractJob job) {
        new JobCreationFilters(job, jobDefaultFilters).runOnCreatedFilter();
    }

    public void runOnCreatingFilter(List<Job> jobs) {
        jobs.forEach(this::runOnCreatingFilter);
    }

    public void runOnCreatedFilter(List<Job> jobs) {
        jobs.forEach(this::runOnCreatedFilter);
    }

    public void runOnStateElectionFilter(Job job) {
        new JobPerformingFilters(job, jobDefaultFilters).runOnStateElectionFilter();
    }

    public void runOnStateAppliedFilters(Job job) {
        new JobPerformingFilters(job, jobDefaultFilters).runOnStateAppliedFilters();
    }

    public void runOnStateElectionFilter(List<Job> jobs) {
        jobs.forEach(this::runOnStateElectionFilter);
    }

    public void runOnStateAppliedFilters(List<Job> jobs) {
        jobs.forEach(this::runOnStateAppliedFilters);
    }
}
