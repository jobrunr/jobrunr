DROP VIEW jobrunr_jobs_stats;
CREATE VIEW jobrunr_jobs_stats AS
 SELECT coalesce(max(( SELECT job_stat_results.count WHERE state IS NULL)), 0)        AS total,
       coalesce(max(( SELECT job_stat_results.count WHERE state = 'SCHEDULED')), 0)  AS scheduled,
       coalesce(max(( SELECT job_stat_results.count WHERE state = 'ENQUEUED')), 0)   AS enqueued,
       coalesce(max(( SELECT job_stat_results.count WHERE state = 'PROCESSING')), 0) AS processing,
       coalesce(max(( SELECT job_stat_results.count WHERE state = 'FAILED')), 0)     AS failed,
       coalesce(max(( SELECT job_stat_results.count WHERE state = 'SUCCEEDED')), 0)  AS succeeded,
       coalesce(( SELECT cast(cast(value AS char(10)) AS decimal(10, 0))
                  FROM jobrunr_metadata jm
                 WHERE jm.id = 'succeeded-jobs-counter-cluster'), 0)                                     AS allTimeSucceeded,
       coalesce(max(( SELECT job_stat_results.count WHERE state = 'DELETED')), 0)    AS deleted,
       ( SELECT count(*)  FROM jobrunr_backgroundjobservers)                                               AS nbrOfBackgroundJobServers,
       ( SELECT count(*)  FROM jobrunr_recurring_jobs)                                                     AS nbrOfRecurringJobs
FROM (SELECT state, count(*) AS count
      FROM jobrunr_jobs
      GROUP BY state
      WITH ROLLUP) job_stat_results;

DROP INDEX jobrunr_job_updated_at_idx ON jobrunr_jobs;
CREATE INDEX jobrunr_jobs_state_updated_idx ON jobrunr_jobs (state ASC, updatedAt ASC);