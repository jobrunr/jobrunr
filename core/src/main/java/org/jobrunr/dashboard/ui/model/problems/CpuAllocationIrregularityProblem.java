package org.jobrunr.dashboard.ui.model.problems;

import org.jobrunr.storage.JobRunrMetadata;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static java.util.stream.Collectors.toCollection;

public class CpuAllocationIrregularityProblem extends Problem {

    public static final String PROBLEM_TYPE = "cpu-allocation-irregularity";
    private final ArrayList<JobRunrMetadata> cpuAllocationIrregularityMetadataSet;
    private final HashSet<String> serversWithCpuAllocationIrregularity;

    protected CpuAllocationIrregularityProblem(List<JobRunrMetadata> cpuAllocationIrregularityMetadataSet) {
        super(PROBLEM_TYPE);
        this.cpuAllocationIrregularityMetadataSet = new ArrayList<>(cpuAllocationIrregularityMetadataSet);
        this.serversWithCpuAllocationIrregularity = cpuAllocationIrregularityMetadataSet.stream()
                .map(jobRunrMetadata -> jobRunrMetadata.getOwner() + " had a CPU Allocation Irregularity of " + jobRunrMetadata.getValue() + " sec around " + jobRunrMetadata.getCreatedAt())
                .collect(toCollection(HashSet::new));
    }

    public HashSet<String> getServersWithCpuAllocationIrregularity() {
        return serversWithCpuAllocationIrregularity;
    }

    public ArrayList<JobRunrMetadata> getCpuAllocationIrregularityMetadataSet() {
        return cpuAllocationIrregularityMetadataSet;
    }
}
