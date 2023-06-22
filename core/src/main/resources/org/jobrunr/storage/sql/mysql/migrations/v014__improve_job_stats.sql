DROP VIEW jobrunr_jobs_stats;
CREATE VIEW jobrunr_jobs_stats AS
SELECT COUNT(*)                                                      AS total,
       COUNT(IF(state = 'SCHEDULED', 1, NULL))                       AS scheduled,
       COUNT(IF(state = 'ENQUEUED', 1, NULL))                        AS enqueued,
       COUNT(IF(state = 'PROCESSING', 1, NULL))                      AS processing,
       COUNT(IF(state = 'FAILED', 1, NULL))                          AS failed,
       COUNT(IF(state = 'SUCCEEDED', 1, NULL))                       AS succeeded,
       COUNT(IF(state = 'DELETED', 1, NULL))                         AS deleted,
       (SELECT COUNT(*) FROM jobrunr_backgroundjobservers)           AS nbrOfBackgroundJobServers,
       (SELECT COUNT(*) FROM jobrunr_recurring_jobs)                 AS nbrOfRecurringJobs,
       COALESCE((SELECT CAST(CAST(value AS char(10)) AS decimal(10, 0))
                 FROM jobrunr_metadata jm
                 WHERE jm.id = 'succeeded-jobs-counter-cluster'), 0) AS allTimeSucceeded
FROM jobrunr_jobs;

DROP INDEX jobrunr_job_updated_at_idx ON jobrunr_jobs;
CREATE INDEX jobrunr_jobs_state_updated_idx ON jobrunr_jobs (state ASC, updatedAt ASC);