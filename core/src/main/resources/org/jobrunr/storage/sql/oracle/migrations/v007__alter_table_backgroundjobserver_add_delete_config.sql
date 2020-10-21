ALTER TABLE jobrunr_backgroundjobservers
    ADD deleteSucceededJobsAfter nvarchar2(64);
ALTER TABLE jobrunr_backgroundjobservers
    ADD permanentlyDeleteJobsAfter nvarchar2(64);