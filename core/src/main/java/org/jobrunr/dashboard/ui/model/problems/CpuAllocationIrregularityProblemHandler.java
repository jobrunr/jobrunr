package org.jobrunr.dashboard.ui.model.problems;

import org.jobrunr.server.dashboard.CpuAllocationIrregularityNotification;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.listeners.MetadataChangeListener;

import java.util.List;

public class CpuAllocationIrregularityProblemHandler implements MetadataChangeListener, ProblemHandler {

    private final Problems problems;
    private final StorageProvider storageProvider;
    private List<JobRunrMetadata> serversWithLongGCCyclesMetadataList;

    public CpuAllocationIrregularityProblemHandler(Problems problems, StorageProvider storageProvider) {
        this.problems = problems;
        this.storageProvider = storageProvider;
        this.storageProvider.addJobStorageOnChangeListener(this);
        this.onChange(storageProvider.getMetadata(CpuAllocationIrregularityNotification.class.getSimpleName()));
    }

    @Override
    public void dismiss() {
        problems.removeProblemsOfType(CpuAllocationIrregularityProblem.PROBLEM_TYPE);
        storageProvider.deleteMetadata(CpuAllocationIrregularityNotification.class.getSimpleName());
    }

    @Override
    public String listenForChangesOfMetadataName() {
        return CpuAllocationIrregularityNotification.class.getSimpleName();
    }

    @Override
    public void onChange(List<JobRunrMetadata> metadataList) {
        if (this.serversWithLongGCCyclesMetadataList == null || this.serversWithLongGCCyclesMetadataList.size() != metadataList.size()) {
            problems.removeProblemsOfType(CpuAllocationIrregularityProblem.PROBLEM_TYPE);
            if (!metadataList.isEmpty()) {
                problems.addProblem(new CpuAllocationIrregularityProblem(metadataList));
            }
            this.serversWithLongGCCyclesMetadataList = metadataList;
        }
    }
}
