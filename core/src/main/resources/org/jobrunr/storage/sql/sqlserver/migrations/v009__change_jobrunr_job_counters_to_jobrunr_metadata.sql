CREATE TABLE jobrunr_metadata
(
    id        VARCHAR(156) PRIMARY KEY,
    name      VARCHAR(92)   NOT NULL,
    owner     VARCHAR(64)   NOT NULL,
    value     NVARCHAR(MAX) NOT NULL,
    createdAt DATETIME2     NOT NULL,
    updatedAt DATETIME2     NOT NULL
);

INSERT INTO jobrunr_metadata (id, name, owner, value, createdAt, updatedAt)
VALUES ('succeeded-jobs-counter-cluster', 'succeeded-jobs-counter', 'cluster',
        cast((select amount from jobrunr_job_counters where name = 'SUCCEEDED') as char(10)), CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

DROP VIEW jobrunr_jobs_stats;
DROP TABLE jobrunr_job_counters;

CREATE VIEW jobrunr_jobs_stats
as
select count(*)                                                                 as total,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'AWAITING')   as awaiting,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'SCHEDULED')  as scheduled,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'ENQUEUED')   as enqueued,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'PROCESSING') as processing,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'FAILED')     as failed,
       (select((select count(*) from jobrunr_jobs jobs where jobs.state = 'SUCCEEDED') +
               (select cast(cast(value as char(10)) as decimal(10, 0))
                from jobrunr_metadata jm
                where jm.id = 'succeeded-jobs-counter-cluster')))               as succeeded,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'DELETED')    as deleted,
       (select count(*) from jobrunr_backgroundjobservers)                      as nbrOfBackgroundJobServers,
       (select count(*) from jobrunr_recurring_jobs)                            as nbrOfRecurringJobs
from jobrunr_jobs j;