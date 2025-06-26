package org.jobrunr.dashboard.ui.model.problems;

import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.utils.InstantUtils;

import java.util.ArrayList;
import java.util.List;

public class CarbonIntensityApiErrorProblem extends Problem {

    public static final String PROBLEM_TYPE = "carbon-intensity-api-error";
    private final ArrayList<JobRunrMetadata> carbonIntensityApiErrorMetadata;

    protected CarbonIntensityApiErrorProblem(List<JobRunrMetadata> metadataList) {
        super(PROBLEM_TYPE, InstantUtils.max(metadataList.stream().map(JobRunrMetadata::getCreatedAt)));
        this.carbonIntensityApiErrorMetadata = new ArrayList<>(metadataList);
    }

    public List<JobRunrMetadata> getCarbonIntensityApiErrorMetadata() {
        return carbonIntensityApiErrorMetadata;
    }

}
