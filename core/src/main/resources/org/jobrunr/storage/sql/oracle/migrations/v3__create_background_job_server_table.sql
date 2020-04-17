CREATE TABLE jobrunr_backgroundjobservers
(
    id                     nchar(36)     NOT NULL,
    workerPoolSize         int           NOT NULL,
    pollIntervalInSeconds  int           NOT NULL,
    firstHeartbeat         TIMESTAMP(6)  NOT NULL,
    lastHeartbeat          TIMESTAMP(6)  NOT NULL,
    running                int           NOT NULL,
    systemTotalMemory      NUMBER(19)    NOT NULL,
    systemFreeMemory       NUMBER(19)    NOT NULL,
    systemCpuLoad          DECIMAL(3, 2) NOT NULL,
    processMaxMemory       NUMBER(19)    NOT NULL,
    processFreeMemory      NUMBER(19)    NOT NULL,
    processAllocatedMemory NUMBER(19)    NOT NULL,
    processCpuLoad         DECIMAL(3, 2) NOT NULL,
    PRIMARY KEY (id)
);
CREATE INDEX jobrunr_bgjobsrvrs_fsthb_idx ON jobrunr_backgroundjobservers (firstHeartbeat);
CREATE INDEX jobrunr_bgjobsrvrs_lsthb_idx ON jobrunr_backgroundjobservers (lastHeartbeat);