package org.jobrunr.utils.jobs;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.SchedulableState;

import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_CREATED_AT;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_SCHEDULED_AT;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_UPDATED_AT;

public class JobSortColumns extends AbstractSortColumns<Job> {
    public JobSortColumns() {
        add(FIELD_UPDATED_AT, Job::getUpdatedAt);
        add(FIELD_CREATED_AT, Job::getCreatedAt);
        add(FIELD_SCHEDULED_AT, job -> job.getJobState() instanceof SchedulableState
                ? ((SchedulableState) job.getJobState()).getScheduledAt()
                : null);
    }
}
