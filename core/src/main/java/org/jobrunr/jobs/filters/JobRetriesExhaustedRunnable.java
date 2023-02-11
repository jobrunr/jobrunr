package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;

@FunctionalInterface
public interface JobRetriesExhaustedRunnable {

    void run(Job job);
}
