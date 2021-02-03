CREATE TABLE jobrunr_metadata
(
    id        NVARCHAR2(156) NOT NULL,
    name      NVARCHAR2(36)  NOT NULL,
    owner     NVARCHAR2(64)  NOT NULL,
    value     clob           NOT NULL,
    createdAt TIMESTAMP(6)   NOT NULL,
    updatedAt TIMESTAMP(6)   NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO jobrunr_metadata (id, name, owner, value, createdAt, updatedAt)
VALUES ('succeeded-jobs-counter-cluster', 'succeeded-jobs-counter', 'cluster',
        cast((select amount from jobrunr_job_counters where name = 'SUCCEEDED') as char(10)), CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

DROP VIEW jobrunr_jobs_stats;
DROP TABLE jobrunr_job_counters;

create view jobrunr_jobs_stats
as
select (select count(*) from jobrunr_jobs jobs)                                 as total,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'AWAITING')   as awaiting,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'SCHEDULED')  as scheduled,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'ENQUEUED')   as enqueued,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'PROCESSING') as processing,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'FAILED')     as failed,
       (select((select count(*) from jobrunr_jobs jobs where jobs.state = 'SUCCEEDED') +
               (select cast(cast(value as char(10)) as decimal(10, 0))
                from jobrunr_metadata jm
                where jm.id = 'succeeded-jobs-counter-cluster'))
        from DUAL)                                                              as succeeded,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'DELETED')    as deleted,
       (select count(*) from jobrunr_backgroundjobservers)                      as nbrOfBackgroundJobServers,
       (select count(*) from jobrunr_recurring_jobs)                            as nbrOfRecurringJobs
from jobrunr_jobs j
group by j.id;