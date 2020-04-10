JobRunr 
=========
<div align="center">

[![Download](https://api.bintray.com/packages/jobrunr/jobrunr/JobRunr/images/download.svg)](https://bintray.com/jobrunr/jobrunr/JobRunr/_latestVersion) [![Build Status](https://drone-jobrunr.dehuysser.be/api/badges/jobrunr/jobrunr/status.svg)](https://drone-jobrunr.dehuysser.be/jobrunr/jobrunr) [![License LGPLv3](https://img.shields.io/badge/license-LGPLv3-green.svg)](http://www.gnu.org/licenses/lgpl-3.0.html)  
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=jobrunr_jobrunr) [![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=reliability_rating)](https://sonarcloud.io/dashboard?id=jobrunr_jobrunr) [![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=security_rating)](https://sonarcloud.io/dashboard?id=jobrunr_jobrunr)  
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=coverage)](https://sonarcloud.io/dashboard?id=jobrunr_jobrunr) [![Bugs](https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=bugs)](https://sonarcloud.io/dashboard?id=jobrunr_jobrunr) [![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=jobrunr_jobrunr)

</div>

## Overview

Incredibly easy way to perform **fire-and-forget**, **delayed** and **recurring jobs** inside **Java applications**. CPU and I/O intensive, long-running and short-running jobs are supported. Persistent storage is done via Postgres, MariaDB/MySQL and Oracle.

JobRunr provides a unified programming model to handle background tasks in a **reliable way** and run them on shared hosting, dedicated hosting or in the cloud within a JVM instance. Some scenario's where it may be a good fit:

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

You can start small and process jobs within your webapp or scale horizontally and add as many background job servers as you want to handle a peak of jobs. JobRunr will distribute the load over all the servers for you. JobRunr is also fault-tolerant - is some webservice down? No worries, the job is automatically retried 10-times with a smart back-off policy.

JobRunr is a Java alternative to [HangFire](https://github.com/HangfireIO/Hangfire), [Resque](https://github.com/resque/resque), [Sidekiq](http://sidekiq.org), [delayed_job](https://github.com/collectiveidea/delayed_job), [Celery](http://www.celeryproject.org) and is similar to [Quartz](https://github.com/quartz-scheduler/quartz) and [Sprint Task Scheduler](https://github.com/spring-guides/gs-scheduling-tasks).

It is also meant to be fast and lean - using it will give you only 3 extra dependencies:
- JobRunr itself
- [asm](https://asm.ow2.io/)
- and you need either [jackson](https://github.com/FasterXML/jackson) and jackson-datatype-jsr310 or [gson](https://github.com/google/gson) on the classpath

Screenshots
-----------
<img src="https://user-images.githubusercontent.com/567842/78462184-f80b0100-76cf-11ea-9462-dd76234e3040.png" width="45%"></img> <img src="https://user-images.githubusercontent.com/567842/78462183-f7726a80-76cf-11ea-8720-d63d840ed3a4.png" width="45%"></img> <img src="https://user-images.githubusercontent.com/567842/78462181-f6d9d400-76cf-11ea-891f-c378d5dd180e.png" width="45%"></img> <img src="https://user-images.githubusercontent.com/567842/78462180-f6413d80-76cf-11ea-8869-0e11ae6d854d.png" width="45%"></img> 

Usage
------

[**Fire-and-forget tasks**](https://www.jobrunr.io/documentation/enqueueing-methods/)

Dedicated worker pool threads execute queued background jobs as soon as possible, shortening your request's processing time.

```java
BackgroundJob.enqueue(() -> System.out.println("Simple!"));
```

[**Delayed tasks**](https://www.jobrunr.io/documentation/scheduling-methods/)

Scheduled background jobs are executed only after a given amount of time.

```java
BackgroundJob.schedule(() -> System.out.println("Reliable!"), Instant.now().plusHours(5));
```

[**Recurring tasks**](https://www.jobrunr.io/documentation/recurring-methods/)

Recurring jobs have never been simpler; just call the following method to perform any kind of recurring task using the [CRON expressions](http://en.wikipedia.org/wiki/Cron#CRON_expression).

```java
BackgroundJob.scheduleRecurringly("my-recurring-job", () -> service.doWork(), Cron.daily());
```

**Process background tasks inside a web application…**

You can process background tasks in any web application and we have thorough support for [Spring](https://spring.io/) - JobRunr is reliable to process your background jobs within a web application.

**… or anywhere else**

Like a Spring Console Application, wrapped in a docker container, that keeps running forever and polls for new background jobs.

See [https://www.jobrunr.io](https://www.jobrunr.io) for more info.

Installation
------------
 
 #### Using Maven?
 
 Add the following to your `pom.xml` to access dependencies of jcenter:
 
 ```xml
<repositories>
    <repository>
      <id>jcenter</id>
      <url>https://jcenter.bintray.com/</url>
    </repository>
</repositories>
```
 
 And finally add the dependency to JobRunr itself
 ```xml
<dependency>
    <groupId>org.jobrunr</groupId>
    <artifactId>jobrunr</artifactId>
    <version>0.8.2</version>
</dependency>
```
 
 #### Using Gradle?
 
Again make sure you depend on jcenter for your dependencies: 
 ```groovy
repositories {
    jcenter()
}
```
 
 And add the dependency to JobRunr itself:
 ```groovy
implementation 'org.jobrunr:jobrunr:0.8.2'
```

Configuration
------------
#### Do you like to work Spring based?

```java
@Bean
public BackgroundJobServer backgroundJobServer(StorageProvider storageProvider, JobActivator jobActivator) {
    final BackgroundJobServer backgroundJobServer = new BackgroundJobServer(storageProvider, jobActivator);
    backgroundJobServer.start();
    return backgroundJobServer;
}

@Bean
public JobActivator jobActivator(ApplicationContext applicationContext) {
    return applicationContext::getBean;
}

@Bean
public JobScheduler jobScheduler(StorageProvider storageProvider) {
    return new JobScheduler(storageProvider);
}

@Bean
public StorageProvider storageProvider(JobMapper jobMapper) {
    final SQLiteDataSource dataSource = new SQLiteDataSource();
    dataSource.setUrl("jdbc:sqlite:" + Paths.get(System.getProperty("java.io.tmpdir"), "jobrunr.db"));
    final SqLiteStorageProvider sqLiteStorageProvider = new SqLiteStorageProvider(dataSource);
    sqLiteStorageProvider.setJobMapper(jobMapper);
    return sqLiteStorageProvider;
}

@Bean
public JobMapper jobMapper(JsonMapper jsonMapper) {
    return new JobMapper(jsonMapper);
}

@Bean
public JsonMapper jsonMapper() {
    return new JacksonJsonMapper(); // or GsonMapper()
}
```

#### Or do you prefer a fluent API?
Define a `javax.sql.DataSource` and put the following code on startup:

```java
@SpringBootApplication
@Import(JobRunrStorageConfiguration.class)
public class WebApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }

    @Bean
    public JobScheduler initJobRunr(ApplicationContext applicationContext) {
        return JobRunr.configure()
                .useJobStorageProvider(SqlJobStorageProviderFactory
                          .using(applicationContext.getBean(DataSource.class)))
                .useJobActivator(applicationContext::getBean)
                .useDefaultBackgroundJobServer()
                .useDashboard()
                .initialize();
    }
}
```
