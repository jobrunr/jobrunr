package org.jobrunr.dashboard.ui.model.problems;

import org.jobrunr.storage.JobRunrMetadata;

import java.util.List;

import static java.util.stream.Collectors.joining;

public class SevereJobRunrExceptionProblem extends Problem {

    public static final String PROBLEM_TYPE = "severe-jobrunr-exception";

    private final String githubIssueTitle;
    private final String githubIssueBody;
    private final int githubIssueBodyLength;

    public SevereJobRunrExceptionProblem(List<JobRunrMetadata> jobRunrMetadataSetWithSevereJobRunrExceptions) {
        super(PROBLEM_TYPE);
        this.githubIssueTitle = "Severe JobRunr Exception";
        this.githubIssueBody = jobRunrMetadataSetWithSevereJobRunrExceptions.stream().map(JobRunrMetadata::getValue).collect(joining("\n\n\n"));
        this.githubIssueBodyLength = githubIssueBody.length();
    }

    public String getGithubIssueTitle() {
        return githubIssueTitle;
    }

    public String getGithubIssueBody() {
        return githubIssueBody;
    }

    public int getGithubIssueBodyLength() {
        return githubIssueBodyLength;
    }
}
