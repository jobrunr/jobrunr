package org.jobrunr.dashboard.ui.model.problems;

import org.jobrunr.server.dashboard.NewJobRunrVersionNotification;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.listeners.MetadataChangeListener;

import java.util.List;

public class NewJobRunrVersionProblemHandler implements MetadataChangeListener, ProblemHandler {

    private final Problems problems;
    private final StorageProvider storageProvider;
    private boolean isNewVersionAvailable;

    public NewJobRunrVersionProblemHandler(Problems problems, StorageProvider storageProvider) {
        this.problems = problems;
        this.storageProvider = storageProvider;
        this.storageProvider.addJobStorageOnChangeListener(this);
        this.onChange(storageProvider.getMetadata(NewJobRunrVersionNotification.class.getSimpleName()));
    }

    @Override
    public void dismiss() {
        throw new IllegalStateException("Problem of type '" + NewJobRunrVersionProblem.PROBLEM_TYPE + "' cannot be dismissed.");
    }

    @Override
    public String listenForChangesOfMetadataName() {
        return NewJobRunrVersionNotification.class.getSimpleName();
    }

    @Override
    public void onChange(List<JobRunrMetadata> metadataList) {
        if (isNewJobRunrVersionAvailable(metadataList)) {
            final JobRunrMetadata jobRunrMetadata = metadataList.get(0);
            problems.addProblem(new NewJobRunrVersionProblem(jobRunrMetadata.getValue()));
            isNewVersionAvailable = true;
        } else if (isJobRunrUpdatedToLatestVersion(metadataList)) {
            problems.removeProblemsOfType(NewJobRunrVersionProblem.PROBLEM_TYPE);
            isNewVersionAvailable = false;
        }
    }

    private boolean isJobRunrUpdatedToLatestVersion(List<JobRunrMetadata> metadataList) {
        return metadataList.isEmpty() && isNewVersionAvailable;
    }

    private boolean isNewJobRunrVersionAvailable(List<JobRunrMetadata> metadataList) {
        return metadataList.size() > 0 && !isNewVersionAvailable;
    }
}
