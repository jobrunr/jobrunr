package org.jobrunr.scheduling

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import org.awaitility.Awaitility.await
import org.awaitility.Durations.TEN_SECONDS
import org.jobrunr.JobRunrAssertions.assertThat
import org.jobrunr.configuration.JobRunr
import org.jobrunr.configuration.JobRunrConfiguration
import org.jobrunr.jobs.annotations.Job
import org.jobrunr.jobs.lambdas.JobRequest
import org.jobrunr.jobs.lambdas.JobRequestHandler
import org.jobrunr.jobs.states.StateName.ENQUEUED
import org.jobrunr.jobs.states.StateName.PROCESSING
import org.jobrunr.jobs.states.StateName.SUCCEEDED
import org.jobrunr.kotlin.utils.mapper.KotlinxSerializationJsonMapper
import org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration
import org.jobrunr.storage.InMemoryStorageProvider
import org.jobrunr.storage.StorageProvider
import org.jobrunr.utils.mapper.JsonMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.internal.util.reflection.Whitebox
import java.time.Duration.ofMillis

class JobRequestSchedulerTest {

    private lateinit var storageProvider: StorageProvider

    @BeforeEach
    fun setUp() {
        storageProvider = InMemoryStorageProvider()
        JobRunr.configure()
            .useStorageProvider(storageProvider)
            .useBackgroundJobServer(usingStandardBackgroundJobServerConfiguration().andPollInterval(ofMillis(200)))
            .initialize()
    }

    @AfterEach
    fun cleanUp() {
        JobRunr.destroy()
    }

    @Serializable
    class AnnotationNotFoundKotlinJobRequest(val input: String) : JobRequest {
        override fun getJobRequestHandler() = AnnotationNotFoundKotlinJobRequestHandler::class.java
    }

    class AnnotationNotFoundKotlinJobRequestHandler : JobRequestHandler<AnnotationNotFoundKotlinJobRequest> {
        @Job(name = "Some neat Job Display Name", retries = 2)
        override fun run(jobRequest: AnnotationNotFoundKotlinJobRequest) {
            println("Running simple job request in background: " + jobRequest.input)
        }
    }

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    @Test
    fun `uses KotlinxSerializationJsonMapper by default`() {
        val jobRunrConfiguration = Whitebox.getInternalState<JobRunrConfiguration>(JobRunr::class.java, "jobRunrConfiguration")
        val jsonMapper = Whitebox.getInternalState<JsonMapper>(jobRunrConfiguration, "jsonMapper")

        assertThat(jsonMapper).isInstanceOf(KotlinxSerializationJsonMapper::class.java)
    }

    @Test
    fun `test enqueue JobRequest with display name where annotation is not found`() {
        val jobId = BackgroundJobRequest.enqueue(AnnotationNotFoundKotlinJobRequest("some input"))

        await().atMost(TEN_SECONDS).until {
            storageProvider.getJobById(jobId).state == SUCCEEDED
        }

        val job = storageProvider.getJobById(jobId)
        assertThat(job)
            .hasJobName("Some neat Job Display Name")
            .hasStates(ENQUEUED, PROCESSING, SUCCEEDED)
    }

    @Serializable
    class AnnotationFoundKotlinJobRequest(val input: String) : JobRequest {
        override fun getJobRequestHandler() = AnnotationFoundKotlinJobRequestHandler::class.java
    }

    class AnnotationFoundKotlinJobRequestHandler : JobRequestHandler<AnnotationFoundKotlinJobRequest> {
        @Job(name = "Some neat Job Display Name", retries = 2)
        override fun run(jobRequest: AnnotationFoundKotlinJobRequest) {
            println("Running simple job request in background: " + jobRequest.input)
        }

        fun doesNotMatter() {
            println("Adding this method ensures the annotation is present")
        }
    }

    @Test
    fun `test enqueue JobRequest with display name where annotation is found`() {
        val jobId = BackgroundJobRequest.enqueue(AnnotationFoundKotlinJobRequest("some input"))

        await().atMost(TEN_SECONDS).until {
            storageProvider.getJobById(jobId).state == SUCCEEDED
        }

        val job = storageProvider.getJobById(jobId)
        assertThat(job)
            .hasJobName("Some neat Job Display Name")
            .hasStates(ENQUEUED, PROCESSING, SUCCEEDED)
    }
}