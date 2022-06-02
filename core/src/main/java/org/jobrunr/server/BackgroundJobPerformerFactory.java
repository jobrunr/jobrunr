package org.jobrunr.server;

import org.jobrunr.jobs.Job;

public interface BackgroundJobPerformerFactory {

    int getPriority();

    BackgroundJobPerformer newBackgroundJobPerformer(BackgroundJobServer backgroundJobServer, Job job);

}
