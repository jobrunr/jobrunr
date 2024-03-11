package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.AbstractJob;

/**
 * A filter that is triggered each time that a Job
 * <ul>
 *     <li>is about to be created (before it is saved to the {@link org.jobrunr.storage.StorageProvider})</li>
 *     <li>has been created (after it has been saved in the {@link org.jobrunr.storage.StorageProvider}</li>
 * </ul>
 *
 * <b><em>Please note:</em></b> Any {@link JobFilter} should process really fast. If it is repeatedly slow, then it will be removed as it negatively impacts the performance of JobRunr.
 */
public interface JobClientFilter extends JobFilter {

    default void onCreating(AbstractJob job) {
    }

    default void onCreated(AbstractJob job) {
    }

}
