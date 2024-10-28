package org.jobrunr.server.tasks.steward;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;

public class UpdateJobsInProgressTask extends AbstractJobStewardTask {

    public UpdateJobsInProgressTask(BackgroundJobServer backgroundJobServer) {
        super(backgroundJobServer);
    }

    @Override
    protected void runTask() {
        LOGGER.debug("Updating currently processed jobs... ");
        convertAndProcessJobs(backgroundJobServer.getJobSteward().getJobsInProgress(), this::updateCurrentlyProcessingJob);
    }

    private Job updateCurrentlyProcessingJob(Job job) {
        try {
            return job.updateProcessing();
        } catch (ClassCastException e) {
            // why: there is a tiny chance that the job already succeeded or failed.
            // For example, if the underlying data structure is a concurrent collection and the iteration is weakly
            // consistent, it might return items in its iterator that have already been removed from the collection.
            return null;
        }
    }
}
