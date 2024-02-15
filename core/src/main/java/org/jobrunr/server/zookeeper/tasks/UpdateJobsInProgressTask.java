package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.JobZooKeeper;

import java.util.ArrayList;

public class UpdateJobsInProgressTask extends ZooKeeperTask {

    public UpdateJobsInProgressTask(JobZooKeeper jobZooKeeper, BackgroundJobServer backgroundJobServer) {
        super(jobZooKeeper, backgroundJobServer);
    }

    @Override
    protected void runTask() {
        LOGGER.debug("Updating currently processed jobs... ");
        convertAndProcessJobs(new ArrayList<>(jobZooKeeper.getJobsInProgress()), this::updateCurrentlyProcessingJob);
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
