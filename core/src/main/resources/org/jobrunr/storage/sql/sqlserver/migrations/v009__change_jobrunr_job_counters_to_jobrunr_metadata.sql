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
		cast((SELECT amount FROM jobrunr_job_counters WHERE name = 'SUCCEEDED') AS char(10)), CURRENT_TIMESTAMP,
		CURRENT_TIMESTAMP);

DROP VIEW jobrunr_jobs_stats;
DROP TABLE jobrunr_job_counters;

CREATE VIEW jobrunr_jobs_stats
AS
SELECT count(*)                                                                 AS total,
	   (SELECT count(*) FROM jobrunr_jobs jobs WHERE jobs.state = 'AWAITING')   AS awaiting,
	   (SELECT count(*) FROM jobrunr_jobs jobs WHERE jobs.state = 'SCHEDULED')  AS scheduled,
	   (SELECT count(*) FROM jobrunr_jobs jobs WHERE jobs.state = 'ENQUEUED')   AS enqueued,
	   (SELECT count(*) FROM jobrunr_jobs jobs WHERE jobs.state = 'PROCESSING') AS processing,
	   (SELECT count(*) FROM jobrunr_jobs jobs WHERE jobs.state = 'FAILED')     AS failed,
	   (SELECT((SELECT count(*) FROM jobrunr_jobs jobs WHERE jobs.state = 'SUCCEEDED') +
			   (SELECT cast(cast(value AS char(10)) AS decimal(10, 0))
				FROM jobrunr_metadata jm
				WHERE jm.id = 'succeeded-jobs-counter-cluster')))               AS succeeded,
	   (SELECT count(*) FROM jobrunr_jobs jobs WHERE jobs.state = 'DELETED')    AS deleted,
	   (SELECT count(*) FROM jobrunr_backgroundjobservers)                      AS nbrOfBackgroundJobServers,
	   (SELECT count(*) FROM jobrunr_recurring_jobs)                            AS nbrOfRecurringJobs
FROM jobrunr_jobs j;