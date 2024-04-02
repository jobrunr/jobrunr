ALTER TABLE jobrunr_jobs
    ADD carbonAwareDeadline DATETIME2;
CREATE INDEX jobrunr_job_carbon_aware_deadline_idx
    ON jobrunr_jobs (carbonAwareDeadline ASC);