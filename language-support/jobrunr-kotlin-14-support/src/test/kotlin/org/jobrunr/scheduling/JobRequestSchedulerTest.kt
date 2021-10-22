package org.jobrunr.scheduling

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.awaitility.Durations.TEN_SECONDS
import org.jobrunr.configuration.JobRunr
import org.jobrunr.jobs.annotations.Job
import org.jobrunr.jobs.lambdas.JobRequest
import org.jobrunr.jobs.lambdas.JobRequestHandler
import org.jobrunr.jobs.states.StateName.*
import org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration
import org.jobrunr.storage.InMemoryStorageProvider
import org.jobrunr.storage.StorageProviderForTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JobRequestSchedulerTest {

    private lateinit var storageProvider: StorageProviderForTest

    @BeforeEach
    fun setUp() {
        storageProvider = StorageProviderForTest(InMemoryStorageProvider())
        JobRunr.configure()
            .useStorageProvider(storageProvider)
            .useBackgroundJobServer(
                usingStandardBackgroundJobServerConfiguration().andPollIntervalInSeconds(5)
            )
            .initialize()
    }

    @AfterEach
    fun cleanUp() {
        JobRunr.destroy()
    }

    class KotlinJobRequest(val input: String) : JobRequest {
        override fun getJobRequestHandler() = KotlinJobRequestHandler::class.java
    }

    class KotlinJobRequestHandler : JobRequestHandler<KotlinJobRequest> {
        @Job(name = "Some neat Job Display Name", retries = 2)
        override fun run(jobRequest: KotlinJobRequest) {
            println("Running simple job request in background: " + jobRequest.input)
        }
    }

    @Test
    fun `test enqueue JobRequest`() {
        val jobId = BackgroundJobRequest.enqueue(KotlinJobRequest("some input"))

        await().atMost(TEN_SECONDS).until {
            storageProvider.getJobById(jobId).state == SUCCEEDED
        }

        val job = storageProvider.getJobById(jobId)
        assertThat(job.jobStates.map { it.name }).containsExactly(
            ENQUEUED, PROCESSING, SUCCEEDED
        )
    }
}