CREATE TABLE jobrunr_metadata
(
    id        NVARCHAR(156) NOT NULL,
    name      NVARCHAR(92)  NOT NULL,
    owner     NVARCHAR(64)  NOT NULL,
    value     clob          NOT NULL,
    createdAt TIMESTAMP(6)  NOT NULL,
    updatedAt TIMESTAMP(6)  NOT NULL,
    PRIMARY KEY (id)
);

INSERT INTO jobrunr_metadata (id, name, owner, value, createdAt, updatedAt)
VALUES ('succeededjobs-cluster', 'succeededjobs', 'cluster',
        cast((select amount from jobrunr_job_counters where name = 'SUCCEEDED') as char(10)), CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

DROP VIEW jobrunr_jobs_stats;
DROP TABLE jobrunr_job_counters;

create view jobrunr_jobs_stats
as
select count(*)                                                                 as total,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'AWAITING')   as awaiting,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'SCHEDULED')  as scheduled,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'ENQUEUED')   as enqueued,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'PROCESSING') as processing,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'FAILED')     as failed,
       ((select count(*) from jobrunr_jobs jobs where jobs.state = 'SUCCEEDED') +
        (select cast(cast(value as char(10)) as decimal(10, 0))
         from jobrunr_metadata jm
         where jm.id = 'succeededjobs-cluster'))                                as succeeded,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'DELETED')    as deleted,
       (select count(*) from jobrunr_backgroundjobservers)                      as nbrOfBackgroundJobServers,
       (select count(*) from jobrunr_recurring_jobs)                            as nbrOfRecurringJobs
from jobrunr_jobs j;