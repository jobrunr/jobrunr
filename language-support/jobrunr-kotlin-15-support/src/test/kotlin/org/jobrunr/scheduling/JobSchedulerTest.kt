package org.jobrunr.scheduling

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.awaitility.Durations
import org.jobrunr.configuration.JobRunr
import org.jobrunr.jobs.mappers.JobMapper
import org.jobrunr.jobs.states.StateName.*
import org.jobrunr.scheduling.cron.Cron
import org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration
import org.jobrunr.server.JobActivator
import org.jobrunr.storage.InMemoryStorageProvider
import org.jobrunr.storage.PageRequest
import org.jobrunr.storage.StorageProviderForTest
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper
import org.junit.jupiter.api.Test
import org.mockito.Mock
import java.time.Instant
import java.time.Instant.now
import java.util.concurrent.TimeUnit

class JobSchedulerTest {

    @Mock
    private val storageProvider = StorageProviderForTest(InMemoryStorageProvider()).also {
        it.setJobMapper(JobMapper(JacksonJsonMapper()))
    }

    private val jobScheduler = JobRunr.configure()
        .useStorageProvider(storageProvider)
        .useJobActivator(object : JobActivator {
            override fun <T : Any> activateJob(type: Class<T>): T? = get(type)
        })
        .useBackgroundJobServer(usingStandardBackgroundJobServerConfiguration().andPollIntervalInSeconds(5))
        .initialize()
        .jobScheduler

    private fun <T> get(type: Class<T>): T? {
        if (type.name == "TestService") {
            return TestService() as T
        } else if (type.name == "org.jobrunr.scheduling.KtJobSchedulerTest\$test enqueue lambda with service dependency\$jobId\$1") {
            throw IllegalArgumentException("Should be TestService, no?")
        }
        return null
    }

    @Test
    fun `test enqueue simple lambda`() {
        val jobId = jobScheduler.enqueue { println("foo") }

        await().atMost(Durations.TEN_SECONDS).until {
            storageProvider.getJobById(jobId).state == SUCCEEDED
        }

        val job = storageProvider.getJobById(jobId)
        assertThat(job.jobStates.map { it.name }).containsExactly(
            ENQUEUED, PROCESSING, SUCCEEDED
        )
    }

    @Test
    fun `test enqueue lambda with service dependency`() {
        val testService = TestService()
        val input = "Hello!"

        val jobId = jobScheduler.enqueue { testService.doWork(input) }

        await().atMost(Durations.TEN_SECONDS).until {
            storageProvider.getJobById(jobId).state == SUCCEEDED
        }

        val job = storageProvider.getJobById(jobId)
        assertThat(job.jobStates.map { it.name }).containsExactly(
            ENQUEUED, PROCESSING, SUCCEEDED
        )
    }

    @Test
    fun `test enqueue lambda with method reference`() {
        val container = TestLambdaContainer()

        val jobId = jobScheduler.enqueue(container::lambdaMethod)

        await().atMost(Durations.TEN_SECONDS).until {
            storageProvider.getJobById(jobId).state == SUCCEEDED
        }

        val job = storageProvider.getJobById(jobId)
        assertThat(job.jobStates.map { it.name }).containsExactly(
            ENQUEUED, PROCESSING, SUCCEEDED
        )
    }

    @Test
    fun `test enqueue lambda with bound local variables`() {
        val amount = 2
        val text = "foo"
        val jobId = jobScheduler.enqueue { println("$text: $amount") }

        await().atMost(Durations.TEN_SECONDS).until {
            storageProvider.getJobById(jobId).state == SUCCEEDED
        }

        val job = storageProvider.getJobById(jobId)
        assertThat(job.jobStates.map { it.name }).containsExactly(
            ENQUEUED, PROCESSING, SUCCEEDED
        )
    }

    @Test
    fun `test schedule lambda `() {
        val amount = 2
        val text = "foo"
        val jobId = BackgroundJob.schedule(now().plusMillis(1000)) { println("$text: $amount") }

        await().atMost(Durations.TEN_SECONDS).until {
            storageProvider.getJobById(jobId).state == SUCCEEDED
        }

        val job = storageProvider.getJobById(jobId)
        assertThat(job.jobStates.map { it.name }).containsExactly(
                SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED
        )
    }

    @Test
    fun `test schedule recurrent job`() {
        val amount = 2
        val text = "foo"

        jobScheduler.scheduleRecurrently(Cron.minutely()) { println("$text: $amount") }

        await().atMost(65, TimeUnit.SECONDS).until {
            storageProvider.countJobs(SUCCEEDED) == 1L
        }

        val job = storageProvider.getJobs(SUCCEEDED, PageRequest.ascOnUpdatedAt(1000))[0]
        assertThat(job.jobStates.map { it.name }).containsExactly(
            SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED
        )
    }

    @Test
    fun `test schedule with polymorphism`() {
        val recurringJob = PrintlnRecurringJob()
        recurringJob.schedule(Cron.minutely())

        await().atMost(65, TimeUnit.SECONDS).until {
            storageProvider.countJobs(SUCCEEDED) == 1L
        }
        val job = storageProvider.getJobs(SUCCEEDED, PageRequest.ascOnUpdatedAt(1000))[0]
        assertThat(job.jobStates.map { it.name }).containsExactly(
            SCHEDULED,
            ENQUEUED,
            PROCESSING,
            SUCCEEDED
        )
    }

    @Test
    fun `test enqueue with top level function`() {
        jobScheduler.enqueue { doSomething() }

        await().until {
            storageProvider.countJobs(SUCCEEDED) == 1L
        }
        val job = storageProvider.getJobs(SUCCEEDED, PageRequest.ascOnUpdatedAt(1000))[0]
        assertThat(job.jobStates.map { it.name }).containsExactly(
            ENQUEUED,
            PROCESSING,
            SUCCEEDED
        )
    }

    fun doSomething() {
        println("hello")
    }

    class TestLambdaContainer {
        private val foo = 2
        private val bar = "foo"
        fun lambdaMethod() = "foo: $foo; bar: $bar"
    }


    interface JobInterface {
        fun call()
    }

    abstract class AbstractRecurringJob : JobInterface {
        fun schedule(cronSchedule: String) {
            BackgroundJob.scheduleRecurrently("test-id", cronSchedule, ::call)
        }
    }

    class PrintlnRecurringJob : AbstractRecurringJob() {

        override fun call() {
            println("In call method")
        }

    }
}