package org.jobrunr.dashboard.ui.model.problems;

import org.jobrunr.SevereJobRunrException;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.listeners.MetadataChangeListener;

import java.util.List;

public class SevereJobRunrExceptionProblemHandler implements MetadataChangeListener, ProblemHandler {

    private final Problems problems;
    private final StorageProvider storageProvider;
    private List<JobRunrMetadata> severeJobRunrExceptionAsMetadataList;

    public SevereJobRunrExceptionProblemHandler(Problems problems, StorageProvider storageProvider) {
        this.problems = problems;
        this.storageProvider = storageProvider;
        this.storageProvider.addJobStorageOnChangeListener(this);
        this.onChange(storageProvider.getMetadata(SevereJobRunrException.class.getSimpleName()));
    }

    @Override
    public void dismiss() {
        problems.removeProblemsOfType(SevereJobRunrExceptionProblem.PROBLEM_TYPE);
        storageProvider.deleteMetadata(SevereJobRunrException.class.getSimpleName());
    }

    @Override
    public String listenForChangesOfMetadataName() {
        return SevereJobRunrException.class.getSimpleName();
    }

    @Override
    public void onChange(List<JobRunrMetadata> metadataList) {
        if (this.severeJobRunrExceptionAsMetadataList == null || this.severeJobRunrExceptionAsMetadataList.size() != metadataList.size()) {
            problems.removeProblemsOfType(SevereJobRunrExceptionProblem.PROBLEM_TYPE);
            if (!metadataList.isEmpty()) {
                problems.addProblem(new SevereJobRunrExceptionProblem(metadataList));
            }
            this.severeJobRunrExceptionAsMetadataList = metadataList;
        }
    }
}
