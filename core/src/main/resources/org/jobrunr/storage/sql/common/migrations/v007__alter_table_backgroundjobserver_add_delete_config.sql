ALTER TABLE jobrunr_backgroundjobservers
    ADD deleteSucceededJobsAfter VARCHAR(64);
ALTER TABLE jobrunr_backgroundjobservers
    ADD permanentlyDeleteJobsAfter VARCHAR(64);