CREATE TABLE jobrunr_recurring_jobs
(
    id        nvarchar(128) NOT NULL,
    version   bigint        NOT NULL,
    jobasjson clob          NOT NULL,
    PRIMARY KEY (id)
)