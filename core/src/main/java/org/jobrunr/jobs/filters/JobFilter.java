package org.jobrunr.jobs.filters;

/**
 * A JobFilter allows to extend JobRunr functionality.
 *
 * <b><em>Please note:</em></b> Any {@link JobFilter} should process really fast. If it is repeatedly slow, then it will be removed as it negatively impacts the performance of JobRunr.
 */
public interface JobFilter {
}
