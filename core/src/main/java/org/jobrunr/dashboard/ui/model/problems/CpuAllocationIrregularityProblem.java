package org.jobrunr.dashboard.ui.model.problems;

import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.utils.InstantUtils;

import java.util.ArrayList;
import java.util.List;

public class CpuAllocationIrregularityProblem extends Problem {

    public static final String PROBLEM_TYPE = "cpu-allocation-irregularity";
    private final ArrayList<JobRunrMetadata> cpuAllocationIrregularityMetadataSet;

    protected CpuAllocationIrregularityProblem(List<JobRunrMetadata> cpuAllocationIrregularityMetadataSet) {
        super(PROBLEM_TYPE, InstantUtils.max(cpuAllocationIrregularityMetadataSet.stream().map(JobRunrMetadata::getCreatedAt)));
        this.cpuAllocationIrregularityMetadataSet = new ArrayList<>(cpuAllocationIrregularityMetadataSet);
    }

    @SuppressWarnings("NonApiType")
    public ArrayList<JobRunrMetadata> getCpuAllocationIrregularityMetadataSet() {
        return cpuAllocationIrregularityMetadataSet;
    }
}
