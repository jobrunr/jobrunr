package org.jobrunr.scheduling

import org.awaitility.Awaitility.await
import org.awaitility.Durations
import org.jobrunr.JobRunrAssertions.assertThat
import org.jobrunr.configuration.JobRunr
import org.jobrunr.jobs.mappers.JobMapper
import org.jobrunr.jobs.states.StateName
import org.jobrunr.scheduling.JobSchedulerTest.JobClientLogFilter
import org.jobrunr.server.BackgroundJobServer
import org.jobrunr.server.BackgroundJobServerConfiguration
import org.jobrunr.storage.InMemoryStorageProvider
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock

class KotlinJobSchedulerTest {
  @Mock
  private val storageProvider = InMemoryStorageProvider().also {
    it.setJobMapper(JobMapper(JacksonJsonMapper()))
  }
  private val jobClientLogFilter = JobClientLogFilter()
  private val jobScheduler = JobScheduler(storageProvider, listOf(jobClientLogFilter))
  private val kotlinJobScheduler = KotlinJobScheduler(jobScheduler)
  private val backgroundJobServer = BackgroundJobServer(
    storageProvider,
    null,
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

  @Test
  fun `test enqueue simple lambda`() {
    val jobId = kotlinJobScheduler.enqueue { "foo" }
    assertThat(jobClientLogFilter.onCreating).isTrue
    assertThat(jobClientLogFilter.onCreated).isTrue
    await().atMost(Durations.FIVE_SECONDS).until {
      storageProvider.getJobById(jobId).state == StateName.SUCCEEDED
    }
    assertThat(storageProvider.getJobById(jobId))
      .hasStates(StateName.ENQUEUED, StateName.PROCESSING, StateName.SUCCEEDED)
  }

  @Test
  fun `test enqueue lambda with bound local variables`() {
    val amount = 2
    val text = "foo"
    val jobId = kotlinJobScheduler.enqueue { "$text: $amount" }
    assertThat(jobClientLogFilter.onCreating).isTrue
    assertThat(jobClientLogFilter.onCreated).isTrue
    await().atMost(Durations.FIVE_SECONDS).until {
      storageProvider.getJobById(jobId).state == StateName.SUCCEEDED
    }
    assertThat(storageProvider.getJobById(jobId))
      .hasStates(StateName.ENQUEUED, StateName.PROCESSING, StateName.SUCCEEDED)
  }
}