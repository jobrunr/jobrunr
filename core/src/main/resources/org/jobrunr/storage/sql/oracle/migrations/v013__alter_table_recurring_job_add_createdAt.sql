ALTER TABLE jobrunr_recurring_jobs
    ADD createdAt NUMBER(19) DEFAULT '0' NOT NULL;
CREATE INDEX jobrunr_recjob_created_at_idx ON jobrunr_recurring_jobs (createdAt);