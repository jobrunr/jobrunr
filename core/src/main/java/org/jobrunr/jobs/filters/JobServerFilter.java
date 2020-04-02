package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;

public interface JobServerFilter extends JobFilter {

    void onProcessing(Job job);

    void onProcessed(Job job);

}
