create table jobrunr_job_counters
(
    name   NCHAR(36) PRIMARY KEY,
    amount int NOT NULL
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