package org.jobrunr.dashboard.ui.model.problems;

import org.jobrunr.storage.JobRunrMetadata;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

class SevereJobRunrExceptionProblemTest {

    @Test
    void githubTitleAndBodyAreCreatedCorrectly() {
        JobRunrMetadata metadata1 = new JobRunrMetadata("not important", "not important", "value 1");
        JobRunrMetadata metadata2 = new JobRunrMetadata("not important", "not important", "value 2");

        SevereJobRunrExceptionProblem severeJobRunrExceptionProblem = new SevereJobRunrExceptionProblem(asList(metadata1, metadata2));

        assertThat(severeJobRunrExceptionProblem.getGithubIssueTitle()).isEqualTo("Severe JobRunr Exception");
        assertThat(severeJobRunrExceptionProblem.getGithubIssueBody()).isEqualTo("value 1\n\n\nvalue 2");
        assertThat(severeJobRunrExceptionProblem.getGithubIssueBodyLength()).isEqualTo(17);
    }
}