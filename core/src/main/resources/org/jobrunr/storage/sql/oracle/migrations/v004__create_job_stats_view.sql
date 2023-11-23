CREATE TABLE jobrunr_job_counters
(
    name   nvarchar2(36) NOT NULL,
    amount int           NOT NULL,
    PRIMARY KEY (name)
);

INSERT INTO jobrunr_job_counters (name, amount)
VALUES ('AWAITING', 0);
INSERT INTO jobrunr_job_counters (name, amount)
VALUES ('SCHEDULED', 0);
INSERT INTO jobrunr_job_counters (name, amount)
VALUES ('ENQUEUED', 0);
INSERT INTO jobrunr_job_counters (name, amount)
VALUES ('PROCESSING', 0);
INSERT INTO jobrunr_job_counters (name, amount)
VALUES ('FAILED', 0);
INSERT INTO jobrunr_job_counters (name, amount)
VALUES ('SUCCEEDED', 0);

CREATE VIEW jobrunr_jobs_stats
as
select (select count(*) from jobrunr_jobs jobs)                                 as total,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'AWAITING')   as awaiting,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'SCHEDULED')  as scheduled,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'ENQUEUED')   as enqueued,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'PROCESSING') as processing,
       (select count(*) from jobrunr_jobs jobs where jobs.state = 'FAILED')     as failed,
       (select((select count(*) from jobrunr_jobs jobs where jobs.state = 'SUCCEEDED') +
               (select amount from jobrunr_job_counters jc where jc.name = 'SUCCEEDED'))
        from DUAL)                                                              as succeeded,
       (select count(*) from jobrunr_backgroundjobservers)                      as nbrOfBackgroundJobServers,
       (select count(*) from jobrunr_recurring_jobs)                            as nbrOfRecurringJobs
from jobrunr_jobs j
group by j.id;