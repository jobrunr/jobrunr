CREATE TABLE jobrunr_recurring_jobs
(
	id        NVARCHAR2(128) NOT NULL,
	version   number(10)     NOT NULL,
	jobAsJson clob           NOT NULL,
	PRIMARY KEY (id)
)