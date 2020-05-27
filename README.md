<p align="center">
 <img src="https://user-images.githubusercontent.com/567842/80095933-1181c900-8569-11ea-85e7-14129b3f8142.png" alt="JobRunr logo"></img>
</p>  
<p align="center">
The ultimate library to perform background processing on the JVM.<br/>
Dead simple API. Extensible. Reliable. <br/>
Distributed and backed by persistent storage. <br/>
Open and free for commercial use.
</p>  
<br/>


<p align="center">
    <a href="https://bintray.com/jobrunr/jobrunr/JobRunr/_latestVersion"><img src="https://api.bintray.com/packages/jobrunr/jobrunr/JobRunr/images/download.svg"></a>&nbsp;
    <img alt="Drone Build" src="https://drone-jobrunr.dehuysser.be/api/badges/jobrunr/jobrunr/status.svg" />&nbsp;
    <img alt="LGPLv3 Licence" src="https://img.shields.io/badge/license-LGPLv3-green.svg" /><br/>
    <a href="https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr"><img alt="Quality Scale" src="https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=sqale_rating" /></a>&nbsp;
    <a href="https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr"><img alt="Reliability Rating" src="https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=reliability_rating" /></a>&nbsp;
    <a href="https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr"><img alt="Security Rating" src="https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=security_rating" /></a><br/>
    <a href="https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr"><img alt="Coverage" src="https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=coverage" /></a>&nbsp;
    <a href="https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr"><img alt="Vulnerabilities" src="https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=vulnerabilities" /></a>&nbsp;
    <a href="https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr"><img alt="Bugs" src="https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=bugs" /></a><br/>
    <a href="https://twitter.com/intent/tweet?text=Try%20JobRunr%20for%20easy%20distributed%20background%20job%20processing%20on%20the%20JVM%21%20&url=https://www.jobrunr.io&via=jobrunr&hashtags=java,scheduling,processing,distributed,developers"><img alt="Tweet about us!" src="https://www.jobrunr.io/assets/image/tweet-btn.svg" /></a>&nbsp;&nbsp;&nbsp;&nbsp;
    <img alt="Star us!" src="https://www.jobrunr.io/assets/image/github-btn.svg" />
    <a href="https://gitter.im/jobrunr/community"><img src="https://badges.gitter.im/jobrunr/general.svg" alt="Join the chat at Gitter" /></a><br />
</p>

## Overview
Incredibly easy way to perform **fire-and-forget**, **delayed** and **recurring jobs** inside **Java applications** using only *Java 8 lambda's*. CPU and I/O intensive, long-running and short-running jobs are supported. Persistent storage is done via either RDBMS (e.g. Postgres, MariaDB/MySQL and Oracle) or NoSQL (MongoDB and Redis).

JobRunr provides a unified programming model to handle background tasks in a **reliable way** and runs them on shared hosting, dedicated hosting or in the cloud (hello Kubernetes) within a JVM instance.


## Features
- Simple: just use Java 8 lambda's to create a background job
- Distributed & cluster-friendly: guarantees execution by single scheduler instance using optimistic locking.
- Persistent jobs: using either a RDMBS (four tables and a view) or a noSQL data store.
- Embeddable: built to be embedded in existing applications.
- Minimal dependencies: ([ASM](https://asm.ow2.io/), slf4j and either [jackson](https://github.com/FasterXML/jackson) and jackson-datatype-jsr310 or [gson](https://github.com/google/gson))

## Usage scenario's
Some scenario's where it may be a good fit:
- within a REST api return response to client immediately and perform long-running job in the background
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

You can start small and process jobs within your webapp or scale horizontally and add as many background job servers as you want to handle a peak of jobs. JobRunr will distribute the load over all the servers for you. JobRunr is also fault-tolerant - is an external webservice down? No worries, the job is automatically retried 10-times with a smart back-off policy.

JobRunr is a Java alternative to [HangFire](https://github.com/HangfireIO/Hangfire), [Resque](https://github.com/resque/resque), [Sidekiq](http://sidekiq.org), [delayed_job](https://github.com/collectiveidea/delayed_job), [Celery](http://www.celeryproject.org) and is similar to [Quartz](https://github.com/quartz-scheduler/quartz) and [Spring Task Scheduler](https://github.com/spring-guides/gs-scheduling-tasks).


Screenshots
-----------
<img src="https://user-images.githubusercontent.com/567842/80217070-60019700-863f-11ea-9f02-d62c77e97a1c.png" width="45%" style="margin-right: 20px;"></img>&nbsp;&nbsp;&nbsp;<img src="https://user-images.githubusercontent.com/567842/80217075-609a2d80-863f-11ea-8994-cd0ca16b31c4.png" width="45%"></img> <br/> <img src="https://user-images.githubusercontent.com/567842/80217067-5f690080-863f-11ea-9d41-3e2878ae7ac8.png" width="45%" style="margin-right: 20px;"></img>&nbsp;&nbsp;&nbsp;<img src="https://user-images.githubusercontent.com/567842/80217063-5ed06a00-863f-11ea-847b-3ed829fd5503.png" width="45%"></img><br /><img src="https://user-images.githubusercontent.com/567842/80217079-6132c400-863f-11ea-9789-8633897ef317.png" width="45%" style="margin-right: 20px;"></img>&nbsp;&nbsp;&nbsp;<img src="https://user-images.githubusercontent.com/567842/80217078-609a2d80-863f-11ea-9b49-c891985de924.png" width="45%"></img> 

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
    <version>0.9.6</version>
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
implementation 'org.jobrunr:jobrunr:0.9.6'
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
                .useStorageProvider(SqlStorageProviderFactory
                          .using(applicationContext.getBean(DataSource.class)))
                .useJobActivator(applicationContext::getBean)
                .useDefaultBackgroundJobServer()
                .useDashboard()
                .initialize();
    }
}
```
## Contributing

See [CONTRIBUTING](https://github.com/jobrunr/jobrunr/blob/master/CONTRIBUTING.md) for details on submitting patches and the contribution workflow.

### How can I contribute?
* Take a look at issues with tag called [`Good first issue`](https://github.com/jobrunr/jobrunr/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22)
* Join the discussion on [gitter](https://gitter.im/jobrunr/community).
* Answer questions on [issues](https://github.com/jobrunr/jobrunr/issues).
* Fix bugs reported on [issues](https://github.com/jobrunr/jobrunr/issues), and send us pull request.
