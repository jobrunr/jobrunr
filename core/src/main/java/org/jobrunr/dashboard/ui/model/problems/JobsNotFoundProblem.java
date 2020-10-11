package org.jobrunr.dashboard.ui.model.problems;

import java.util.HashSet;
import java.util.Set;

public class JobsNotFoundProblem extends Problem {

    private HashSet<String> jobsNotFound;

    public JobsNotFoundProblem(Set<String> jobsNotFound) {
        super("jobs-not-found");
        this.jobsNotFound = new HashSet<>(jobsNotFound);
    }

    public Set<String> getJobsNotFound() {
        return jobsNotFound;
    }
}
