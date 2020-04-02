package org.jobrunr.server.runner;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobContext;

public class RunnerJobContext extends JobContext {

    public RunnerJobContext(Job job) {
        super(job);
    }

}
