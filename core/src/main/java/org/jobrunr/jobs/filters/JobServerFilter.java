package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;

/**
 * A filter that is triggered each time that a Job starts processing or has been processed.
 * Can be useful for adding extra logging, ... .
 */
public interface JobServerFilter extends JobFilter {

    default void onProcessing(Job job) {}

    default void onProcessed(Job job) {}

}
