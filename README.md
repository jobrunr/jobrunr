JobRunr 
=========

## Overview

Incredibly easy way to perform **fire-and-forget**, **delayed** and **recurring jobs** inside **Java applications**. CPU and I/O intensive, long-running and short-running jobs are supported. Persistent storage is done via Postgres, MariaDB/MySQL and Oracle.

JobRunr provides a unified programming model to handle background tasks in a **reliable way** and run them on shared hosting, dedicated hosting or in the cloud. You can start with a simple setup and grow computational power for background jobs with time for these scenarios:

- mass notifications/newsletters
- calculations of wages and the creation of the resulting documents
- batch import from xml, csv or json
- creation of archives
- firing off web hooks
- image/video processing
- purging temporary files
- recurring automated reports
- database maintenance
- updating elasticsearch/solr after data changes 
- *…and so on*

JobRunr is a Java alternative to [HangFire](https://github.com/HangfireIO/Hangfire), [Resque](https://github.com/resque/resque), [Sidekiq](http://sidekiq.org), [delayed_job](https://github.com/collectiveidea/delayed_job), [Celery](http://www.celeryproject.org) and similar to [Quartz](https://github.com/quartz-scheduler/quartz), [Sprint Task Scheduler](https://github.com/spring-guides/gs-scheduling-tasks).

More info will follow soon