CREATE TABLE jobrunr_jobs
(
    id           NCHAR(36) PRIMARY KEY,
    version      int           NOT NULL,
    jobAsJson    NVARCHAR(MAX) NOT NULL,
    jobSignature NVARCHAR(512) NOT NULL,
    state        VARCHAR(36)   NOT NULL,
    createdAt    DATETIME2     NOT NULL,
    updatedAt    DATETIME2     NOT NULL,
    scheduledAt  DATETIME2
);
CREATE INDEX jobrunr_state_idx ON jobrunr_jobs (state);
CREATE INDEX jobrunr_job_signature_idx ON jobrunr_jobs (jobSignature);
CREATE INDEX jobrunr_job_created_at_idx ON jobrunr_jobs (createdAt);
CREATE INDEX jobrunr_job_updated_at_idx ON jobrunr_jobs (updatedAt);
CREATE INDEX jobrunr_job_scheduled_at_idx ON jobrunr_jobs (scheduledAt);