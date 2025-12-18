package org.jobrunr.scheduling

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import org.assertj.core.api.Assertions.assertThatCode
import org.awaitility.Awaitility.await
import org.awaitility.Durations
import org.jobrunr.JobRunrAssertions.assertThat
import org.jobrunr.configuration.JobRunr
import org.jobrunr.jobs.mappers.JobMapper
import org.jobrunr.jobs.states.StateName.ENQUEUED
import org.jobrunr.jobs.states.StateName.PROCESSING
import org.jobrunr.jobs.states.StateName.SCHEDULED
import org.jobrunr.jobs.states.StateName.SUCCEEDED
import org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration
import org.jobrunr.server.JobActivator
import org.jobrunr.storage.InMemoryStorageProvider
import org.jobrunr.storage.Paging.AmountBasedList.ascOnUpdatedAt
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper
import org.junit.jupiter.api.Test
import org.mockito.Mock
import java.time.Duration.ofMillis
import java.time.Instant.now
import java.util.concurrent.TimeUnit

class JobSchedulerTest {

    @Mock
    private val storageProvider = InMemoryStorageProvider().also {
        it.setJobMapper(JobMapper(JacksonJsonMapper()))
    }

    private val jobScheduler = JobRunr.configure()
        .useStorageProvider(storageProvider)
        .useJobActivator(object : JobActivator {
            override fun <T : Any> activateJob(type: Class<T>): T? = get(type)
        })
        .useBackgroundJobServer(usingStandardBackgroundJobServerConfiguration().andPollInterval(ofMillis(200)))
        .initialize()
        .jobScheduler

    private fun <T> get(type: Class<T>): T? {
        if (type.name == "TestService") {
            return type.cast(TestService())
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
        assertThat(job).hasStates(ENQUEUED, PROCESSING, SUCCEEDED)
    }

    @Test
    fun `test enqueue lambda with default parameter throws exception`() {
        val testService = TestService()
        assertThatCode { jobScheduler.enqueue { testService.doWorkWithDefaultParameter() } }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Unsupported lambda")
            .hasRootCauseMessage("You are (probably) using Kotlin default parameter values which is not supported by JobRunr.")
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
        assertThat(job).hasStates(ENQUEUED, PROCESSING, SUCCEEDED)
    }

    @Test
    fun `test enqueue lambda with service dependency and job name`() {
        val testService = TestService()
        val input = "Hello!"

        val jobId = jobScheduler.enqueue { testService.doWorkWithJobName(input, "Hello") }

        await().atMost(Durations.TEN_SECONDS).until {
            storageProvider.getJobById(jobId).state == SUCCEEDED
        }

        val job = storageProvider.getJobById(jobId)
        assertThat(job)
            .hasJobName("Some neat Job Display Name")
            .hasStates(ENQUEUED, PROCESSING, SUCCEEDED)
    }

    @Test
    fun `test enqueue lambda with method reference`() {
        val container = TestLambdaContainer()

        val jobId = jobScheduler.enqueue(container::lambdaMethod)

        await().atMost(Durations.TEN_SECONDS).until {
            storageProvider.getJobById(jobId).state == SUCCEEDED
        }

        val job = storageProvider.getJobById(jobId)
        assertThat(job)
            .hasStates(ENQUEUED, PROCESSING, SUCCEEDED)
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
        assertThat(job)
            .hasStates(ENQUEUED, PROCESSING, SUCCEEDED)
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
        assertThat(job)
            .hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED)
    }

    @Test
    fun `test schedule recurrent job`() {
        val amount = 2
        val text = "foo"

        jobScheduler.scheduleRecurrently("*/2 * * * * *") { println("$text: $amount") }

        await().atMost(35, TimeUnit.SECONDS).until {
            storageProvider.countJobs(SUCCEEDED) >= 1L
        }

        val job = storageProvider.getJobList(SUCCEEDED, ascOnUpdatedAt(1000))[0]
        assertThat(job)
            .hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED)
    }

    @Test
    fun `test schedule with polymorphism`() {
        val recurringJob = PrintlnRecurringJob()
        recurringJob.schedule("*/2 * * * * *")

        await().atMost(35, TimeUnit.SECONDS).until {
            storageProvider.countJobs(SUCCEEDED) >= 1L
        }
        val job = storageProvider.getJobList(SUCCEEDED, ascOnUpdatedAt(1000))[0]
        assertThat(job)
            .hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED)
    }

    @Test
    fun `test enqueue with top level function`() {
        jobScheduler.enqueue { doSomething() }

        await().until {
            storageProvider.countJobs(SUCCEEDED) == 1L
        }
        val job = storageProvider.getJobList(SUCCEEDED, ascOnUpdatedAt(1000))[0]
        assertThat(job)
            .hasStates(ENQUEUED, PROCESSING, SUCCEEDED)
    }

    @Test
    fun enqueueWithJsonCreator() {
        val param = ExampleWrapper(1)
        jobScheduler.enqueue { doWork(param) }

        await().until {
            storageProvider.countJobs(SUCCEEDED) == 1L
        }
        val job = storageProvider.getJobList(SUCCEEDED, ascOnUpdatedAt(1000))[0]
        assertThat(job)
            .hasStates(ENQUEUED, PROCESSING, SUCCEEDED)
    }

    @Test
    fun enqueueJobWithPayload() {
        enqueueBugJob(JobPayload(2, 1))
    }

    fun enqueueBugJob(jobPayload: JobPayload) {
        val testService = TestService()

        val funId = jobPayload.someId
        val anotherFunId = jobPayload.anotherId
        jobScheduler.enqueue {
            testService.doSomethingWithIds(funId, anotherFunId)
        }
    }

    fun doSomething() {
        println("hello")
    }

    fun doWork(example: ExampleWrapper) {
        println("Hello " + example.value)
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

    class ExampleWrapper @JsonCreator constructor(
        @get:JsonValue val value: Int
    )

    data class JobPayload(val someId: Long, val anotherId: Long)
}