DROP VIEW jobrunr_jobs_stats;
CREATE VIEW jobrunr_jobs_stats
as
with job_stat_results as (SELECT state, count(*) as count
                          FROM jobrunr_jobs
                          GROUP BY state)
select coalesce((select sum(job_stat_results.count) from job_stat_results), 0)                            as total,
       coalesce((select sum(job_stat_results.count) from job_stat_results where state = 'AWAITING'), 0)   as awaiting,
       coalesce((select sum(job_stat_results.count) from job_stat_results where state = 'SCHEDULED'), 0)  as scheduled,
       coalesce((select sum(job_stat_results.count) from job_stat_results where state = 'ENQUEUED'), 0)   as enqueued,
       coalesce((select sum(job_stat_results.count) from job_stat_results where state = 'PROCESSING'), 0) as processing,
       coalesce((select sum(job_stat_results.count) from job_stat_results where state = 'PROCESSED'), 0)  as processed,
       coalesce((select sum(job_stat_results.count) from job_stat_results where state = 'FAILED'), 0)     as failed,
       coalesce((select sum(job_stat_results.count) from job_stat_results where state = 'SUCCEEDED'), 0)  as succeeded,
       coalesce((select cast(cast(value as char(10)) as decimal(10, 0))
                 from jobrunr_metadata jm
                 where jm.id = 'succeeded-jobs-counter-cluster'), 0)                                      as allTimeSucceeded,
       coalesce((select sum(job_stat_results.count) from job_stat_results where state = 'DELETED'), 0)    as deleted,
       (select count(*) from jobrunr_backgroundjobservers)                                                as nbrOfBackgroundJobServers,
       (select count(*) from jobrunr_recurring_jobs)                                                      as nbrOfRecurringJobs;