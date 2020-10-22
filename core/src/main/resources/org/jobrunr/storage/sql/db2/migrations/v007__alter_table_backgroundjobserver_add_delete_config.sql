ALTER TABLE jobrunr_backgroundjobservers
    ADD deleteSucceededJobsAfter nvarchar(32);
ALTER TABLE jobrunr_backgroundjobservers
    ADD permanentlyDeleteJobsAfter nvarchar(32);