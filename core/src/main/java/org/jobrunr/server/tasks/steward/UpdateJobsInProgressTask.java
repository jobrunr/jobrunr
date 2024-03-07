package org.jobrunr.server.tasks.steward;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;

import java.util.ArrayList;

public class UpdateJobsInProgressTask extends AbstractJobStewardTask {

    public UpdateJobsInProgressTask(BackgroundJobServer backgroundJobServer) {
        super(backgroundJobServer);
    }

    @Override
    protected void runTask() {
        LOGGER.debug("Updating currently processed jobs... ");
        convertAndProcessJobs(new ArrayList<>(backgroundJobServer.getJobSteward().getJobsInProgress()), this::updateCurrentlyProcessingJob);
    }

    private Job updateCurrentlyProcessingJob(Job job) {
        try {
            return job.updateProcessing();
        } catch (ClassCastException e) {
            // why: because of thread context switching there is a tiny chance that the job already succeeded or failed.
            return null;
        }
    }
}
