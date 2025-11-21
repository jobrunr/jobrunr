DROP VIEW jobrunr_jobs_stats;
CREATE VIEW jobrunr_jobs_stats
AS
with job_stat_results AS (SELECT state, count(*) AS count
    FROM jobrunr_jobs
    GROUP BY ROLLUP (state
)
)
SELECT coalesce((SELECT count FROM job_stat_results WHERE state IS NULL), 0)        AS total,
       coalesce((SELECT count FROM job_stat_results WHERE state = 'SCHEDULED'), 0)  AS scheduled,
       coalesce((SELECT count FROM job_stat_results WHERE state = 'ENQUEUED'), 0)   AS enqueued,
       coalesce((SELECT count FROM job_stat_results WHERE state = 'PROCESSING'), 0) AS processing,
       coalesce((SELECT count FROM job_stat_results WHERE state = 'FAILED'), 0)     AS failed,
       coalesce((SELECT count FROM job_stat_results WHERE state = 'SUCCEEDED'), 0)  AS succeeded,
       coalesce((SELECT cast(cast(value AS char(10)) AS decimal(10, 0))
                 FROM jobrunr_metadata jm
                 WHERE jm.id = 'succeeded-jobs-counter-cluster'), 0)                AS allTimeSucceeded,
       coalesce((SELECT count FROM job_stat_results WHERE state = 'DELETED'), 0)    AS deleted,
       (SELECT count(*) FROM jobrunr_backgroundjobservers)                          AS nbrOfBackgroundJobServers,
       (SELECT count(*) FROM jobrunr_recurring_jobs)                                AS nbrOfRecurringJobs;

DROP INDEX jobrunr_job_updated_at_idx;
CREATE INDEX jobrunr_jobs_state_updated_idx ON jobrunr_jobs (state ASC, updatedAt ASC);