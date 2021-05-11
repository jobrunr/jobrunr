package org.jobrunr.dashboard.ui.model.problems;

import java.util.HashSet;
import java.util.Set;

public class ScheduledJobsNotFoundProblem extends Problem {

    public static final String PROBLEM_TYPE = "jobs-not-found";

    private HashSet<String> jobsNotFound;

    public ScheduledJobsNotFoundProblem(Set<String> jobsNotFound) {
        super(PROBLEM_TYPE);
        this.jobsNotFound = new HashSet<>(jobsNotFound);
    }

    public Set<String> getJobsNotFound() {
        return jobsNotFound;
    }
}
