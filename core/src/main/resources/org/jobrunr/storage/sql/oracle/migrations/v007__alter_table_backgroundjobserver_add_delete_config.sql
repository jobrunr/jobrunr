ALTER TABLE jobrunr_backgroundjobservers
    ADD deleteSucceededJobsAfter nvarchar2(32);
ALTER TABLE jobrunr_backgroundjobservers
    ADD permanentlyDeleteJobsAfter nvarchar2(32);