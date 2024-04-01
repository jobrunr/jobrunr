ALTER TABLE jobrunr_jobs
    ADD COLUMN carbonAwareDeadline TIMESTAMP(6);

CREATE INDEX jobrunr_job_carbon_aware_deadline_idx ON jobrunr_jobs (carbonAwareDeadline);
