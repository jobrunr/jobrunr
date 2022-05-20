ALTER TABLE jobrunr_recurring_jobs
    ADD createdAt BIGINT NOT NULL DEFAULT '0';
CREATE INDEX jobrunr_recurring_job_created_at_idx ON jobrunr_recurring_jobs (createdAt);