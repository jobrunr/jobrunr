CREATE TABLE jobrunr_migrations
(
    id          nchar(36)    NOT NULL,
    script      nvarchar(64) NOT NULL,
    installedon nvarchar(29) NOT NULL,
    PRIMARY KEY (id)
)