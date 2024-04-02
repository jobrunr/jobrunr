ALTER TABLE jobrunr_jobs
    ADD carbonAwareDeadline TIMESTAMP;
CREATE INDEX jobrunr_job_carbon_aware_deadline_idx
    ON jobrunr_jobs (carbonAwareDeadline ASC);