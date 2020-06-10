CREATE TABLE jobrunr_jobs
(
    id           nchar(36)      NULL,
    version      number(10)     NOT NULL,
    jobasjson    clob           NOT NULL,
    jobSignature NVARCHAR2(512) NOT NULL,
    state        NVARCHAR2(36)  NOT NULL,
    createdAt    TIMESTAMP(6)   NOT NULL,
    updatedAt    TIMESTAMP(6)   NOT NULL,
    scheduledAt  TIMESTAMP(6),
    PRIMARY KEY (id)
);
CREATE INDEX jobrunr_state_idx ON jobrunr_jobs (state);
CREATE INDEX jobrunr_job_signature_idx ON jobrunr_jobs (jobSignature);
CREATE INDEX jobrunr_job_created_at_idx ON jobrunr_jobs (createdAt);
CREATE INDEX jobrunr_job_updated_at_idx ON jobrunr_jobs (updatedAt);
CREATE INDEX jobrunr_job_scheduled_at_idx ON jobrunr_jobs (scheduledAt);