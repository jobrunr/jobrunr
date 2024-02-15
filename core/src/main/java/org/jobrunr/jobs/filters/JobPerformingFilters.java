package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.utils.streams.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;

public class JobPerformingFilters extends AbstractJobFilters {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobPerformingFilters.class);

    private final Job job;

    public JobPerformingFilters(Job job, JobDefaultFilters jobFilters) {
        super(job, jobFilters);
        this.job = job;
    }

    public void runOnStateElectionFilter() {
        if(!job.hasStateChange()) return;
        electStateFilters().forEach(catchThrowable(electStateFilter -> electStateFilter.onStateElection(job, job.getJobState())));
    }

    public void runOnStateAppliedFilters() {
        List<JobState> stateChanges = job.getStateChangesForJobFilters();
        if (stateChanges.isEmpty()) return;
        applyStateFilters().forEach(catchThrowable(applyStateFilter -> {
            for (int i = stateChanges.size(); i >= 1; i--) {
                applyStateFilter.onStateApplied(job, job.getJobState(-(i + 1)), job.getJobState(-(i)));
            }
        }));
    }

    public void runOnJobProcessingFilters() {
        jobServerFilters().forEach(catchThrowable(jobServerFilter -> jobServerFilter.onProcessing(job)));
    }

    public void runOnJobProcessingSucceededFilters() {
        jobServerFilters().forEach(catchThrowable(jobServerFilter -> jobServerFilter.onProcessingSucceeded(job)));
    }

    public void runOnJobProcessingFailedFilters(Exception e) {
        jobServerFilters().forEach(catchThrowable(jobServerFilter -> jobServerFilter.onProcessingFailed(job, e)));
    }

    public void runOnJobFailedAfterRetriesFilters() {
        jobServerFilters().forEach(catchThrowable(jobServerFilter -> jobServerFilter.onFailedAfterRetries(job)));
    }

    private Stream<ElectStateFilter> electStateFilters() {
        return electStateFilters(jobFilters);
    }

    private Stream<ApplyStateFilter> applyStateFilters() {
        return applyStateFilters(jobFilters);
    }

    private Stream<JobServerFilter> jobServerFilters() {
        return jobServerFilters(jobFilters);
    }

    private static Stream<ElectStateFilter> electStateFilters(List<JobFilter> jobFilters) {
        return StreamUtils.ofType(jobFilters, ElectStateFilter.class);
    }

    private static Stream<ApplyStateFilter> applyStateFilters(List<JobFilter> jobFilters) {
        return StreamUtils.ofType(jobFilters, ApplyStateFilter.class);
    }

    private static Stream<JobServerFilter> jobServerFilters(List<JobFilter> jobFilters) {
        return StreamUtils.ofType(jobFilters, JobServerFilter.class);
    }

    @Override
    Logger getLogger() {
        return LOGGER;
    }

}
