DROP VIEW jobrunr_jobs_stats;

CREATE VIEW jobrunr_jobs_stats
AS
SELECT count(*)                                                                 AS total,
       (SELECT count(*) FROM jobrunr_jobs jobs WHERE jobs.state = 'AWAITING')   AS awaiting,
       (SELECT count(*) FROM jobrunr_jobs jobs WHERE jobs.state = 'SCHEDULED')  AS scheduled,
       (SELECT count(*) FROM jobrunr_jobs jobs WHERE jobs.state = 'ENQUEUED')   AS enqueued,
       (SELECT count(*) FROM jobrunr_jobs jobs WHERE jobs.state = 'PROCESSING') AS processing,
       (SELECT count(*) FROM jobrunr_jobs jobs WHERE jobs.state = 'FAILED')     AS failed,
       (SELECT count(*) FROM jobrunr_jobs jobs WHERE jobs.state = 'SUCCEEDED')  AS succeeded,
       (SELECT cast(cast(value AS char(10)) AS decimal(10, 0))
        FROM jobrunr_metadata jm
        WHERE jm.id = 'succeeded-jobs-counter-cluster')                         AS allTimeSucceeded,
       (SELECT count(*) FROM jobrunr_jobs jobs WHERE jobs.state = 'DELETED')    AS deleted,
       (SELECT count(*) FROM jobrunr_backgroundjobservers)                      AS nbrOfBackgroundJobServers,
       (SELECT count(*) FROM jobrunr_recurring_jobs)                            AS nbrOfRecurringJobs
FROM jobrunr_jobs j
group by j.id;