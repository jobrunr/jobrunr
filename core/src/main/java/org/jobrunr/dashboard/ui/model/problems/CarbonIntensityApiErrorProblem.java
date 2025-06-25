package org.jobrunr.dashboard.ui.model.problems;

import org.jobrunr.storage.JobRunrMetadata;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CarbonIntensityApiErrorProblem extends Problem {

    public static final String PROBLEM_TYPE = "carbon-intensity-api-error";
    private final ArrayList<JobRunrMetadata> carbonIntensityApiErrorMetadata;

    protected CarbonIntensityApiErrorProblem(List<JobRunrMetadata> metadataList) {
        super(PROBLEM_TYPE);
        this.carbonIntensityApiErrorMetadata = new ArrayList<>(metadataList);
    }

    public List<JobRunrMetadata> getCarbonIntensityApiErrorMetadata() {
        return carbonIntensityApiErrorMetadata;
    }

    protected CarbonIntensityApiErrorProblem(List<JobRunrMetadata> metadataList, Instant createdAt) {
        super(PROBLEM_TYPE, createdAt);
        this.carbonIntensityApiErrorMetadata = new ArrayList<>(metadataList);
    }

}
