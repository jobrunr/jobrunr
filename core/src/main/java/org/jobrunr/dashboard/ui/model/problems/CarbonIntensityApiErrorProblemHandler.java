package org.jobrunr.dashboard.ui.model.problems;

import org.jobrunr.server.dashboard.CarbonIntensityApiErrorNotification;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.listeners.MetadataChangeListener;
import org.jobrunr.utils.InstantUtils;

import java.util.List;

public class CarbonIntensityApiErrorProblemHandler implements MetadataChangeListener, ProblemHandler {

    private final Problems problems;
    private final StorageProvider storageProvider;
    private List<JobRunrMetadata> metadata;

    public CarbonIntensityApiErrorProblemHandler(Problems problems, StorageProvider storageProvider) {
        this.problems = problems;
        this.storageProvider = storageProvider;
        this.storageProvider.addJobStorageOnChangeListener(this);
        this.onChange(storageProvider.getMetadata(listenForChangesOfMetadataName()));
    }

    @Override
    public void dismiss() {
        problems.removeProblemsOfType(CarbonIntensityApiErrorProblem.PROBLEM_TYPE);
        storageProvider.deleteMetadata(CarbonIntensityApiErrorNotification.class.getSimpleName());
    }

    @Override
    public String listenForChangesOfMetadataName() {
        return CarbonIntensityApiErrorNotification.class.getSimpleName();
    }

    @Override
    public void onChange(List<JobRunrMetadata> metadataList) {
        if (this.metadata == null || this.metadata.size() != metadataList.size()) {
            problems.removeProblemsOfType(CarbonIntensityApiErrorProblem.PROBLEM_TYPE);
            if (!metadataList.isEmpty()) {
                problems.addProblem(new CarbonIntensityApiErrorProblem(metadataList, InstantUtils.max(metadataList.stream().map(JobRunrMetadata::getCreatedAt))));
            }
            this.metadata = metadataList;
        }
    }
}
