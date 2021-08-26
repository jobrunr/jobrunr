package org.jobrunr.dashboard.ui.model.problems;

public class NewJobRunrVersionProblem extends Problem {

    public static final String PROBLEM_TYPE = "new-jobrunr-version";

    private final String latestVersion;

    protected NewJobRunrVersionProblem(String latestVersion) {
        super(PROBLEM_TYPE);
        this.latestVersion = latestVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
