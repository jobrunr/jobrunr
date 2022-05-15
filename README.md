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
    <a href="https://search.maven.org/artifact/org.jobrunr/jobrunr"><img src="https://maven-badges.herokuapp.com/maven-central/org.jobrunr/jobrunr/badge.svg"></a>&nbsp;
    <img alt="Drone Build" src="https://build.jobrunr.io/api/badges/jobrunr/jobrunr/status.svg" />&nbsp;
    <img alt="LGPLv3 Licence" src="https://img.shields.io/badge/license-LGPLv3-green.svg" /><br/>
    <a href="https://sonarcloud.io/dashboard?id=jobrunr_jobrunr"><img alt="Quality Scale" src="https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=sqale_rating" /></a>&nbsp;
    <a href="https://sonarcloud.io/dashboard?id=jobrunr_jobrunr"><img alt="Reliability Rating" src="https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=reliability_rating" /></a>&nbsp;
    <a href="https://sonarcloud.io/dashboard?id=jobrunr_jobrunr"><img alt="Security Rating" src="https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=security_rating" /></a><br/>
    <a href="https://sonarcloud.io/dashboard?id=jobrunr_jobrunr"><img alt="Coverage" src="https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=coverage" /></a>&nbsp;
    <a href="https://sonarcloud.io/dashboard?id=jobrunr_jobrunr"><img alt="Vulnerabilities" src="https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=vulnerabilities" /></a>&nbsp;
    <a href="https://sonarcloud.io/dashboard?id=jobrunr_jobrunr"><img alt="Bugs" src="https://sonarcloud.io/api/project_badges/measure?project=jobrunr_jobrunr&metric=bugs" /></a><br/>
    <a href="https://twitter.com/intent/tweet?text=Try%20JobRunr%20for%20easy%20distributed%20background%20job%20processing%20on%20the%20JVM%21%20&url=https://www.jobrunr.io&via=jobrunr&hashtags=java,scheduling,processing,distributed,developers"><img alt="Tweet about us!" src="https://www.jobrunr.io/tweet-btn.svg?v2" /></a>&nbsp;
    <a href="https://github.com/jobrunr/jobrunr/stargazers"><img alt="Star us!" src="https://www.jobrunr.io/github-star-btn.svg?v2" /></a>
    <a href="https://github.com/jobrunr/jobrunr/discussions"><img src="https://img.shields.io/badge/chat-Github%20discussions-green" alt="Join the chat at Gitter" /></a><br />
</p>

## Overview
```java
BackgroundJob.enqueue(() -> System.out.println("This is all you need for distributed jobs!"));
```

Incredibly easy way to perform **fire-and-forget**, **delayed**, **scheduled** and **recurring jobs** inside **Java applications** using only *Java 8 lambda's*. CPU and I/O intensive, long-running and short-running jobs are supported. Persistent storage is done via either RDBMS (e.g. Postgres, MariaDB/MySQL, Oracle, SQL Server, DB2 and SQLite) or NoSQL (ElasticSearch, MongoDB and Redis).

JobRunr provides a unified programming model to handle background tasks in a **reliable way** and runs them on shared hosting, dedicated hosting or in the cloud (hello Kubernetes) within a JVM instance.


## Feedback
> Thanks for building JobRunr, I like it a lot! Before that I used similar libraries in Ruby and Golang and JobRunr so far is the most pleasant one to use. I especially like the dashboard, it’s awesome! [Alex Denisov](https://www.linkedin.com/in/alex-denisov-a29bab2a/)

View more feedback on [jobrunr.io](https://www.jobrunr.io/en/#why-jobrunr).


## Features
- Simple: just use Java 8 lambda's to create a background job.
- Distributed & cluster-friendly: guarantees execution by single scheduler instance using optimistic locking.
- Persistent jobs: using either a RDMBS (four tables and a view) or a noSQL data store.
- Embeddable: built to be embedded in existing applications.
- Minimal dependencies: ([ASM](https://asm.ow2.io/), slf4j and either [jackson](https://github.com/FasterXML/jackson) and jackson-datatype-jsr310, [gson](https://github.com/google/gson) or a JSON-B compliant library).

## Usage scenarios
Some scenarios where it may be a good fit:
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

You can start small and process jobs within your web app or scale horizontally and add as many background job servers as you want to handle a peak of jobs. JobRunr will distribute the load over all the servers for you. JobRunr is also fault-tolerant - is an external web service down? No worries, the job is automatically retried 10-times with a smart back-off policy.

JobRunr is a Java alternative to [HangFire](https://github.com/HangfireIO/Hangfire), [Resque](https://github.com/resque/resque), [Sidekiq](http://sidekiq.org), [delayed_job](https://github.com/collectiveidea/delayed_job), [Celery](https://github.com/celery/celery) and is similar to [Quartz](https://github.com/quartz-scheduler/quartz) and [Spring Task Scheduler](https://github.com/spring-guides/gs-scheduling-tasks).


Screenshots
-----------
<img src="https://user-images.githubusercontent.com/567842/80217070-60019700-863f-11ea-9f02-d62c77e97a1c.png" width="45%" style="margin-right: 20px;"></img>&nbsp;&nbsp;&nbsp;<img src="https://user-images.githubusercontent.com/567842/80217075-609a2d80-863f-11ea-8994-cd0ca16b31c4.png" width="45%"></img> <br/> <img src="https://user-images.githubusercontent.com/567842/80217067-5f690080-863f-11ea-9d41-3e2878ae7ac8.png" width="45%" style="margin-right: 20px;"></img>&nbsp;&nbsp;&nbsp;<img src="https://user-images.githubusercontent.com/567842/80217063-5ed06a00-863f-11ea-847b-3ed829fd5503.png" width="45%"></img><br /><img src="https://user-images.githubusercontent.com/567842/80217079-6132c400-863f-11ea-9789-8633897ef317.png" width="45%" style="margin-right: 20px;"></img>&nbsp;&nbsp;&nbsp;<img src="https://user-images.githubusercontent.com/567842/80217078-609a2d80-863f-11ea-9b49-c891985de924.png" width="45%"></img> 

Usage
------

[**Fire-and-forget tasks**](https://www.jobrunr.io/en/documentation/background-methods/enqueueing-jobs/)

Dedicated worker pool threads execute queued background jobs as soon as possible, shortening your request's processing time.

```java
BackgroundJob.enqueue(() -> System.out.println("Simple!"));
```

[**Delayed tasks**](https://www.jobrunr.io/en/documentation/background-methods/scheduling-jobs/)

Scheduled background jobs are executed only after a given amount of time.

```java
BackgroundJob.schedule(Instant.now().plusHours(5), () -> System.out.println("Reliable!"));
```

[**Recurring tasks**](https://www.jobrunr.io/en/documentation/background-methods/recurring-jobs/)

Recurring jobs have never been simpler; just call the following method to perform any kind of recurring task using the [CRON expressions](http://en.wikipedia.org/wiki/Cron#CRON_expression).

```java
BackgroundJob.scheduleRecurringly("my-recurring-job", Cron.daily(), () -> service.doWork());
```

**Process background tasks inside a web application…**

You can process background tasks in any web application and we have thorough support for [Spring](https://spring.io/) - JobRunr is reliable to process your background jobs within a web application.

**… or anywhere else**

Like a Spring Console Application, wrapped in a docker container, that keeps running forever and polls for new background jobs.

See [https://www.jobrunr.io](https://www.jobrunr.io) for more info.

Installation
------------
 
 #### Using Maven?
 
 JobRunr is available in Maven Central - all you need to do is add the following dependency:
 
 ```xml
<dependency>
    <groupId>org.jobrunr</groupId>
    <artifactId>jobrunr</artifactId>
    <version>5.1.0</version>
</dependency>
```
 
 #### Using Gradle?
 
Just add the dependency to JobRunr:
 ```groovy
implementation 'org.jobrunr:jobrunr:5.1.0'
```

Configuration
------------
#### Do you like to work Spring based?

Add the [*jobrunr-spring-boot-starter*](https://search.maven.org/artifact/org.jobrunr/jobrunr-spring-boot-starter) to your dependencies and you're almost ready to go! Just set up your `application.properties`:

```
# the job-scheduler is enabled by default
# the background-job-server and dashboard are disabled by default
org.jobrunr.job-scheduler.enabled=true
org.jobrunr.background-job-server.enabled=true
org.jobrunr.dashboard.enabled=true
```

#### Or do you prefer a fluent API?
Define a `javax.sql.DataSource` and put the following code on startup:

```java
@SpringBootApplication
public class JobRunrApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobRunrApplication.class, args);
    }

    @Bean
    public JobScheduler initJobRunr(DataSource dataSource, JobActivator jobActivator) {
        return JobRunr.configure()
                .useJobActivator(jobActivator)
                .useStorageProvider(SqlStorageProviderFactory
                          .using(dataSource))
                .useBackgroundJobServer()
                .useDashboard()
                .initialize();
    }
}
```
## Contributing

See [CONTRIBUTING](https://github.com/jobrunr/jobrunr/blob/master/CONTRIBUTING.md) for details on submitting patches and the contribution workflow.

### How can I contribute?
* Take a look at issues with tag called [`Good first issue`](https://github.com/jobrunr/jobrunr/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22)
* Join the discussion on [Github discussion](https://github.com/jobrunr/jobrunr/discussions) - we won't be using Gitter anymore.
* Answer questions on [issues](https://github.com/jobrunr/jobrunr/issues).
* Fix bugs reported on [issues](https://github.com/jobrunr/jobrunr/issues), and send us pull request.

### How to build?
* `git clone https://github.com/jobrunr/jobrunr.git`
* `cd jobrunr`
* `cd core/src/main/resources/org/jobrunr/dashboard/frontend`
* `npm i`
* `npm run build`
* `cd -`
* `./gradlew publishToMavenLocal`

Then, in your own project you can depend on `org.jobrunr:jobrunr:1.0.0-SNAPSHOT`.
