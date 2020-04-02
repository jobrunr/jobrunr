package org.jobrunr.server.runner;

import org.jobrunr.jobs.Job;

public interface BackgroundJobRunner {

    boolean supports(Job job);

    void run(Job job) throws Exception;

}
