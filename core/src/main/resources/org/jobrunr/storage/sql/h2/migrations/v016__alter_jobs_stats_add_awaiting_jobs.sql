DROP VIEW jobrunr_jobs_stats;

CREATE VIEW jobrunr_jobs_stats AS
SELECT COALESCE(SUM(count), 0)                                                AS total,
       COALESCE(SUM(CASE WHEN state = 'AWAITING' THEN count ELSE 0 END), 0)   AS awaiting,
       COALESCE(SUM(CASE WHEN state = 'SCHEDULED' THEN count ELSE 0 END), 0)  AS scheduled,
       COALESCE(SUM(CASE WHEN state = 'ENQUEUED' THEN count ELSE 0 END), 0)   AS enqueued,
       COALESCE(SUM(CASE WHEN state = 'PROCESSING' THEN count ELSE 0 END), 0) AS processing,
       COALESCE(SUM(CASE WHEN state = 'PROCESSED' THEN count ELSE 0 END), 0)  AS processed,
       COALESCE(SUM(CASE WHEN state = 'FAILED' THEN count ELSE 0 END), 0)     AS failed,
       COALESCE(SUM(CASE WHEN state = 'SUCCEEDED' THEN count ELSE 0 END), 0)  AS succeeded,
       COALESCE((SELECT CAST(CAST(`value` AS CHAR(10)) AS DECIMAL(10, 0))
                 FROM jobrunr_metadata jm
                 WHERE jm.id = 'succeeded-jobs-counter-cluster'), 0)          AS allTimeSucceeded,
       COALESCE(SUM(CASE WHEN state = 'DELETED' THEN count ELSE 0 END), 0)    AS deleted,
       (SELECT COUNT(*) FROM jobrunr_backgroundjobservers)                    AS nbrOfBackgroundJobServers,
       (SELECT COUNT(*) FROM jobrunr_recurring_jobs)                          AS nbrOfRecurringJobs
FROM (SELECT state, COUNT(*) AS count
      FROM jobrunr_jobs
      GROUP BY state);