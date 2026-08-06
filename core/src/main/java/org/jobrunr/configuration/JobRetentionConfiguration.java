package org.jobrunr.configuration;

import org.jobrunr.jobs.states.StateName;

import java.time.Duration;
import java.util.Objects;

public class JobRetentionConfiguration {

    public static final Duration DEFAULT_DELETE_SUCCEEDED_JOBS_DURATION = Duration.ofHours(36);
    public static final Duration DEFAULT_PERMANENTLY_DELETE_JOBS_DURATION = Duration.ofHours(72);

    private final Duration deleteSucceededJobsAfter;
    private final Duration permanentlyDeleteDeletedJobsAfter;

    /**
     * Creates a {@code JobRetentionConfiguration} with the default retention durations.
     * <p>
     * Succeeded jobs are moved to {@link StateName#DELETED} after {@link #DEFAULT_DELETE_SUCCEEDED_JOBS_DURATION},
     * and jobs in the {@link StateName#DELETED} state are permanently removed from storage after {@link #DEFAULT_PERMANENTLY_DELETE_JOBS_DURATION}.
     *
     * @see #JobRetentionConfiguration(Duration, Duration)
     */
    public JobRetentionConfiguration() {
        this(DEFAULT_DELETE_SUCCEEDED_JOBS_DURATION, DEFAULT_PERMANENTLY_DELETE_JOBS_DURATION);
    }

    /**
     * Creates a {@code JobRetentionConfiguration} with the given retention durations.
     * <p>
     * Succeeded jobs are first moved to {@link StateName#DELETED}; they are only removed from
     * storage once they have been in that state for {@code permanentlyDeleteDeletedJobsAfter}.
     * Each duration is measured from the moment the job entered its current state.
     *
     * @param deleteSucceededJobsAfter          how long a succeeded job is retained before it is
     *                                          moved to {@link StateName#DELETED}; must not be {@code null}
     * @param permanentlyDeleteDeletedJobsAfter how long a job stays in {@link StateName#DELETED} before it
     *                                          is permanently removed from storage; must not be {@code null}
     * @throws NullPointerException if {@code deleteSucceededJobsAfter} or
     *                              {@code permanentlyDeleteDeletedJobsAfter} is {@code null}
     */
    public JobRetentionConfiguration(Duration deleteSucceededJobsAfter, Duration permanentlyDeleteDeletedJobsAfter) {
        Objects.requireNonNull(deleteSucceededJobsAfter, "deleteSucceededJobsAfter must not be null");
        Objects.requireNonNull(permanentlyDeleteDeletedJobsAfter, "permanentlyDeleteDeletedJobsAfter must not be null");
        this.deleteSucceededJobsAfter = deleteSucceededJobsAfter;
        this.permanentlyDeleteDeletedJobsAfter = permanentlyDeleteDeletedJobsAfter;
    }

    public Duration getDeleteSucceededJobsAfter() {
        return deleteSucceededJobsAfter;
    }

    public Duration getPermanentlyDeleteDeletedJobsAfter() {
        return permanentlyDeleteDeletedJobsAfter;
    }
}
