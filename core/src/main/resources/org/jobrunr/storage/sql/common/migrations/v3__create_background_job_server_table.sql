CREATE TABLE jobrunr_backgroundjobservers
(
    id                    NCHAR(36) PRIMARY KEY,
    workerPoolSize        int          NOT NULL,
    pollIntervalInSeconds int          NOT NULL,
    firstHeartbeat        TIMESTAMP(6) NOT NULL,
    lastHeartbeat         TIMESTAMP(6) NOT NULL,
    running               int          not null
);
CREATE INDEX jobrunr_bgjobsrvrs_fsthb_idx ON jobrunr_backgroundjobservers (firstHeartbeat);
CREATE INDEX jobrunr_bgjobsrvrs_lsthb_idx ON jobrunr_backgroundjobservers (lastHeartbeat);