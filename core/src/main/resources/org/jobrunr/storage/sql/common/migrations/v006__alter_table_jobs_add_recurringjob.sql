ALTER TABLE jobrunr_jobs
    ADD recurringJobId VARCHAR(128);
CREATE INDEX jobrunr_job_rci_idx ON jobrunr_jobs (recurringJobId);