package org.jobrunr.utils.jobs;

import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.storage.StorageProviderUtils;

public class RecurringJobSortColumns extends AbstractSortColumns<RecurringJob> {
    public RecurringJobSortColumns() {
        add(StorageProviderUtils.RecurringJobs.FIELD_ID, RecurringJob::getId);
        add(StorageProviderUtils.RecurringJobs.FIELD_CREATED_AT, RecurringJob::getCreatedAt);
    }
}
