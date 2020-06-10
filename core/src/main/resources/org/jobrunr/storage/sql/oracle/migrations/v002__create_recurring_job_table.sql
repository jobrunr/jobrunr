CREATE TABLE jobrunr_recurring_jobs
(
    id        nvarchar2(128) NOT NULL,
    version   number(10)     NOT NULL,
    jobasjson clob           NOT NULL,
    PRIMARY KEY (id)
)