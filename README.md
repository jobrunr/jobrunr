JobRunr 
=========
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=jobrunr_jobrunr) [![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=jobrunr_jobrunr) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=security_rating)](https://sonarcloud.io/dashboard?id=jobrunr_jobrunr) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=coverage)](https://sonarcloud.io/dashboard?id=jobrunr_jobrunr) [![Bugs](https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=bugs)](https://sonarcloud.io/dashboard?id=jobrunr_jobrunr) [![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=jobrunr_jobrunr) [![License LGPLv3](https://img.shields.io/badge/license-LGPLv3-green.svg)](http://www.gnu.org/licenses/lgpl-3.0.html)

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

JobRunr is a Java alternative to [HangFire](https://github.com/HangfireIO/Hangfire), [Resque](https://github.com/resque/resque), [Sidekiq](http://sidekiq.org), [delayed_job](https://github.com/collectiveidea/delayed_job), [Celery](http://www.celeryproject.org) and is similar to [Quartz](https://github.com/quartz-scheduler/quartz) and [Sprint Task Scheduler](https://github.com/spring-guides/gs-scheduling-tasks).

Usage
------

This is an incomplete list of features. The website with documentation will follow soon.

[**Fire-and-forget tasks**]

Dedicated worker pool threads execute queued background jobs as soon as possible, shortening your request's processing time.

```java
BackgroundJob.enqueue(() => System.out.println("Simple!"));
```

[**Delayed tasks**]

Scheduled background jobs are executed only after a given amount of time.

```java
BackgroundJob.schedule(() => System.out.println("Reliable!"), Instant.now().plusHours(5));
```

[**Recurring tasks**]

Recurring jobs have never been simpler; just call the following method to perform any kind of recurring task using the [CRON expressions](http://en.wikipedia.org/wiki/Cron#CRON_expression).

```java
BackgroundJob.scheduleRecurringly("my-recurring-job", () -> service.doWork(), Cron.daily());
```

**Process background tasks inside a web application…**

You can process background tasks in any web application and we have thorough support for [Spring](https://spring.io/) - JobRunr is reliable for web applications from scratch, even on shared hosting.

```java
    JobRunr.configure()
           .useJobStorageProvider(jobStorageProvider)
           .useJobActivator(jobActivator)
           .useDefaultBackgroundJobServer()
           .useDashboard()
           .initialize();
```

**… or anywhere else**
