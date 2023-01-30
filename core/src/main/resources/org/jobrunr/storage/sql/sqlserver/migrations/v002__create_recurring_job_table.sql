CREATE TABLE jobrunr_recurring_jobs
(
    id        NCHAR(128) PRIMARY KEY,
    version   int  NOT NULL,
    jobAsJson NVARCHAR(MAX) NOT NULL
);