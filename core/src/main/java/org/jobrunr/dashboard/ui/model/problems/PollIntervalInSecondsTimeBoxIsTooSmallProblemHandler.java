package org.jobrunr.dashboard.ui.model.problems;

import org.jobrunr.server.dashboard.PollIntervalInSecondsTimeBoxIsTooSmallNotification;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.listeners.MetadataChangeListener;

import java.util.List;

public class PollIntervalInSecondsTimeBoxIsTooSmallProblemHandler implements MetadataChangeListener, ProblemHandler {

    private final Problems problems;
    private final StorageProvider storageProvider;
    private List<JobRunrMetadata> serversWithPollIntervalInSecondsTimeBoxTooSmallMetadataList;

    public PollIntervalInSecondsTimeBoxIsTooSmallProblemHandler(Problems problems, StorageProvider storageProvider) {
        this.problems = problems;
        this.storageProvider = storageProvider;
        this.storageProvider.addJobStorageOnChangeListener(this);
        this.onChange(storageProvider.getMetadata(PollIntervalInSecondsTimeBoxIsTooSmallNotification.class.getSimpleName()));
    }

    @Override
    public void dismiss() {
        problems.removeProblemsOfType(PollIntervalInSecondsTimeBoxIsTooSmallProblem.PROBLEM_TYPE);
        storageProvider.deleteMetadata(PollIntervalInSecondsTimeBoxIsTooSmallNotification.class.getSimpleName());
    }

    @Override
    public String listenForChangesOfMetadataName() {
        return PollIntervalInSecondsTimeBoxIsTooSmallNotification.class.getSimpleName();
    }

    @Override
    public void onChange(List<JobRunrMetadata> metadataList) {
        if (this.serversWithPollIntervalInSecondsTimeBoxTooSmallMetadataList == null || this.serversWithPollIntervalInSecondsTimeBoxTooSmallMetadataList.size() != metadataList.size()) {
            problems.removeProblemsOfType(PollIntervalInSecondsTimeBoxIsTooSmallProblem.PROBLEM_TYPE);
            if (!metadataList.isEmpty()) {
                problems.addProblem(new PollIntervalInSecondsTimeBoxIsTooSmallProblem(metadataList));
            }
            this.serversWithPollIntervalInSecondsTimeBoxTooSmallMetadataList = metadataList;
        }
    }
}
