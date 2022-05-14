package org.jobrunr.dashboard.ui.model.problems;

import org.jobrunr.storage.JobRunrMetadata;

import java.util.ArrayList;
import java.util.List;

public class PollIntervalInSecondsTimeBoxIsTooSmallProblem extends Problem {

    public static final String PROBLEM_TYPE = "poll-interval-in-seconds-is-too-small";
    private final ArrayList<JobRunrMetadata> pollIntervalInSecondsTimeBoxIsTooSmallMetadataSet;

    protected PollIntervalInSecondsTimeBoxIsTooSmallProblem(List<JobRunrMetadata> pollIntervalInSecondsTimeBoxIsTooSmallMetadataSet) {
        super(PROBLEM_TYPE);
        this.pollIntervalInSecondsTimeBoxIsTooSmallMetadataSet = new ArrayList<>(pollIntervalInSecondsTimeBoxIsTooSmallMetadataSet);
    }

    public ArrayList<JobRunrMetadata> getPollIntervalInSecondsTimeBoxIsTooSmallMetadataSet() {
        return pollIntervalInSecondsTimeBoxIsTooSmallMetadataSet;
    }
}
