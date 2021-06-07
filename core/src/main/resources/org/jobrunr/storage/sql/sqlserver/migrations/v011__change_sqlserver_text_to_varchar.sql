ALTER TABLE jobrunr_jobs
    ALTER COLUMN jobAsJson NVARCHAR(MAX);
ALTER TABLE jobrunr_jobs
    ALTER COLUMN jobSignature NVARCHAR(512);
ALTER TABLE jobrunr_metadata
    ALTER COLUMN value NVARCHAR(MAX);