package org.jobrunr.dashboard.ui.model.problems;

import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.listeners.JobStatsChangeListener;

import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.jobrunr.utils.JobUtils.jobExists;

public class ScheduledJobsNotFoundProblemHandler implements JobStatsChangeListener, ProblemHandler {

    private final Problems problems;
    private final StorageProvider storageProvider;
    private JobStats jobStats;

    public ScheduledJobsNotFoundProblemHandler(Problems problems, StorageProvider storageProvider) {
        this.problems = problems;
        this.storageProvider = storageProvider;
        this.initScheduledJobNotFoundProblems();
    }

    @Override
    public void dismiss() {
        throw new IllegalStateException("Problem of type '" + ScheduledJobsNotFoundProblem.PROBLEM_TYPE + "' cannot be dismissed.");
    }

    @Override
    public void onChange(JobStats jobStats) {
        if (this.jobStats == null || jobStats.getScheduled() < this.jobStats.getScheduled()) {
            initScheduledJobNotFoundProblems();
            this.jobStats = jobStats;
        }
    }

    private void initScheduledJobNotFoundProblems() {
        problems.removeProblemsOfType(ScheduledJobsNotFoundProblem.PROBLEM_TYPE);
        Set<String> jobsThatCannotBeFoundAnymore = storageProvider.getDistinctJobSignatures(StateName.SCHEDULED).stream().filter(jobSignature -> !jobExists(jobSignature)).collect(toSet());
        if (!jobsThatCannotBeFoundAnymore.isEmpty()) {
            storageProvider.addJobStorageOnChangeListener(this);
            jobStats = storageProvider.getJobStats();
            problems.addProblem(new ScheduledJobsNotFoundProblem(jobsThatCannotBeFoundAnymore));
        } else {
            storageProvider.removeJobStorageOnChangeListener(this);
        }
    }


}
