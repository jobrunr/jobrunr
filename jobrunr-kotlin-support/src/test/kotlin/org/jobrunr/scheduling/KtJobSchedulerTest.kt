package org.jobrunr.scheduling

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.awaitility.Durations
import org.jobrunr.configuration.JobRunr
import org.jobrunr.jobs.AbstractJob
import org.jobrunr.jobs.filters.JobClientFilter
import org.jobrunr.jobs.mappers.JobMapper
import org.jobrunr.jobs.states.StateName
import org.jobrunr.scheduling.cron.Cron
import org.jobrunr.server.BackgroundJobServer
import org.jobrunr.server.BackgroundJobServerConfiguration
import org.jobrunr.server.JobActivator
import org.jobrunr.storage.InMemoryStorageProvider
import org.jobrunr.storage.PageRequest
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.Mock
import java.util.concurrent.TimeUnit

class KtJobSchedulerTest {
    class JobClientLogFilter : JobClientFilter {
        var onCreating = false
        var onCreated = false

        override fun onCreating(job: AbstractJob) {
            onCreating = true
        }

        override fun onCreated(job: AbstractJob) {
            onCreated = true
        }
    }

    @Mock
    private val storageProvider = InMemoryStorageProvider().also {
        it.setJobMapper(JobMapper(JacksonJsonMapper()))
    }
    private val jobClientLogFilter = JobClientLogFilter()
    private val jobScheduler = KtJobScheduler(JobScheduler(storageProvider, listOf(jobClientLogFilter)))
    private val backgroundJobServer = BackgroundJobServer(
            storageProvider,
            object : JobActivator {
                override fun <T : Any> activateJob(type: Class<T>): T? = get(type)
            },
            BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration()
                    .andPollIntervalInSeconds(5)
    )

    @BeforeEach
    fun setUp() {
        JobRunr.configure()
                .useStorageProvider(storageProvider)
                .useBackgroundJobServer(backgroundJobServer)
                .initialize()
        backgroundJobServer.start()
    }

    private fun <T> get(type: Class<T>): T? {
        if (type.name == "TestService") {
            return TestService() as T
        } else if (type.name == "org.jobrunr.scheduling.KtJobSchedulerTest\$test enqueue lambda with service dependency\$jobId\$1") {
            throw IllegalArgumentException("Should be TestService, no?");
        }
        return null
    }

    @Test
    fun `test enqueue simple lambda`() {
        val jobId = jobScheduler.enqueue { println("foo") }
        assertTrue(jobClientLogFilter.onCreating)
        assertTrue(jobClientLogFilter.onCreated)
        await().atMost(Durations.FIVE_SECONDS).until {
            storageProvider.getJobById(jobId).state == StateName.SUCCEEDED
        }
        val job = storageProvider.getJobById(jobId)
        assertThat(job.jobStates.map { it.name }).containsExactly(
                StateName.ENQUEUED,
                StateName.PROCESSING,
                StateName.SUCCEEDED
        )
    }

    @Test
    fun `test enqueue lambda with service dependency`() {
        val testService = TestService()
        val input = "Hello!"
        val jobId = jobScheduler.enqueue { testService.doWork(input) }
        await().atMost(Durations.FIVE_SECONDS).until {
            storageProvider.getJobById(jobId).state == StateName.SUCCEEDED
        }
        val job = storageProvider.getJobById(jobId)
        assertThat(job.jobStates.map { it.name }).containsExactly(
                StateName.ENQUEUED,
                StateName.PROCESSING,
                StateName.SUCCEEDED
        )
    }

    @Test
    fun `test enqueue lambda with bound local variables`() {
        val amount = 2
        val text = "foo"
        val jobId = jobScheduler.enqueue { "$text: $amount" }
        assertTrue(jobClientLogFilter.onCreating)
        assertTrue(jobClientLogFilter.onCreated)
        await().atMost(Durations.FIVE_SECONDS).until {
            storageProvider.getJobById(jobId).state == StateName.SUCCEEDED
        }
        val job = storageProvider.getJobById(jobId)
        assertThat(job.jobStates.map { it.name }).containsExactly(
                StateName.ENQUEUED, StateName.PROCESSING, StateName.SUCCEEDED
        )
    }

    @Test
    fun `test schedule recurrent job`() {
        val amount = 2
        val text = "foo"
        jobScheduler.scheduleRecurrently(Cron.minutely()) { println("$text: $amount") }
        assertThat(jobClientLogFilter.onCreating).isTrue
        assertThat(jobClientLogFilter.onCreated).isTrue
        await().atMost(65, TimeUnit.SECONDS).until {
            storageProvider.countJobs(StateName.SUCCEEDED) == 1L
        }
        val job = storageProvider.getJobs(StateName.SUCCEEDED, PageRequest.ascOnUpdatedAt(1000))[0]
        assertThat(job.jobStates.map { it.name }).containsExactly(
                StateName.SCHEDULED,
                StateName.ENQUEUED,
                StateName.PROCESSING,
                StateName.SUCCEEDED
        )
    }

    class TestLambdaContainer {
        private val foo = 2
        private val bar = "foo"
        fun lambdaMethod() = "foo: $foo; bar: $bar"
    }

    @Disabled
    @Test
    fun `test enqueue lambda with method reference`() {
        val container = TestLambdaContainer()
        val jobId = jobScheduler.enqueue(container::lambdaMethod)
        assertThat(jobClientLogFilter.onCreating).isTrue
        assertThat(jobClientLogFilter.onCreated).isTrue
        await().atMost(Durations.FIVE_SECONDS).until {
            storageProvider.getJobById(jobId).state == StateName.SUCCEEDED
        }
        val job = storageProvider.getJobById(jobId)
        assertThat(job.jobStates.map { it.name }).containsExactly(
                StateName.ENQUEUED, StateName.PROCESSING, StateName.SUCCEEDED
        )
    }
}