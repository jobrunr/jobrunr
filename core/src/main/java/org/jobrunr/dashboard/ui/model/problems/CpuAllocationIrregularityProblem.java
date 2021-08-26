package org.jobrunr.dashboard.ui.model.problems;

import org.jobrunr.storage.JobRunrMetadata;

import java.util.ArrayList;
import java.util.List;

public class CpuAllocationIrregularityProblem extends Problem {

    public static final String PROBLEM_TYPE = "cpu-allocation-irregularity";
    private final ArrayList<JobRunrMetadata> cpuAllocationIrregularityMetadataSet;

    protected CpuAllocationIrregularityProblem(List<JobRunrMetadata> cpuAllocationIrregularityMetadataSet) {
        super(PROBLEM_TYPE);
        this.cpuAllocationIrregularityMetadataSet = new ArrayList<>(cpuAllocationIrregularityMetadataSet);
    }

    public ArrayList<JobRunrMetadata> getCpuAllocationIrregularityMetadataSet() {
        return cpuAllocationIrregularityMetadataSet;
    }
}
