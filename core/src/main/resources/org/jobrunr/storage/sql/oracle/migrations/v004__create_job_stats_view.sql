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
AS
SELECT (SELECT count(*) FROM jobrunr_jobs jobs)                                 AS total,
	   (SELECT count(*) FROM jobrunr_jobs jobs WHERE jobs.state = 'AWAITING')   AS awaiting,
	   (SELECT count(*) FROM jobrunr_jobs jobs WHERE jobs.state = 'SCHEDULED')  AS scheduled,
	   (SELECT count(*) FROM jobrunr_jobs jobs WHERE jobs.state = 'ENQUEUED')   AS enqueued,
	   (SELECT count(*) FROM jobrunr_jobs jobs WHERE jobs.state = 'PROCESSING') AS processing,
	   (SELECT count(*) FROM jobrunr_jobs jobs WHERE jobs.state = 'FAILED')     AS failed,
	   (SELECT((SELECT count(*) FROM jobrunr_jobs jobs WHERE jobs.state = 'SUCCEEDED') +
			   (SELECT amount FROM jobrunr_job_counters jc WHERE jc.name = 'SUCCEEDED'))
		FROM DUAL)                                                              AS succeeded,
	   (SELECT count(*) FROM jobrunr_backgroundjobservers)                      AS nbrOfBackgroundJobServers,
	   (SELECT count(*) FROM jobrunr_recurring_jobs)                            AS nbrOfRecurringJobs
FROM jobrunr_jobs j
group by j.id;