ALTER TABLE jobrunr_backgroundjobservers
    ADD deleteSucceededJobsAfter nvarchar(64);
ALTER TABLE jobrunr_backgroundjobservers
    ADD permanentlyDeleteJobsAfter nvarchar(64);