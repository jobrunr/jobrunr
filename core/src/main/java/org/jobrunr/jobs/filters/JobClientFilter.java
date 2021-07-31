package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.AbstractJob;

/**
 * A filter that is triggered each time that a Job
 * <ul>
 *     <li>is about to be created (before it is saved to the {@link org.jobrunr.storage.StorageProvider})</li>
 *     <li>has been created (after is has been saved in the {@link org.jobrunr.storage.StorageProvider}</li>
 * </ul>
 */
public interface JobClientFilter extends JobFilter {

    default void onCreating(AbstractJob job) {}

    default void onCreated(AbstractJob job) {}

}
