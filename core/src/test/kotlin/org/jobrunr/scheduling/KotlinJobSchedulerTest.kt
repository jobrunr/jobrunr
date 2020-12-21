package org.jobrunr.scheduling

import org.awaitility.Awaitility.await
import org.awaitility.Durations
import org.jobrunr.JobRunrAssertions.assertThat
import org.jobrunr.configuration.JobRunr
import org.jobrunr.jobs.mappers.JobMapper
import org.jobrunr.jobs.states.StateName
import org.jobrunr.scheduling.JobSchedulerTest.JobClientLogFilter
import org.jobrunr.scheduling.cron.Cron
import org.jobrunr.server.BackgroundJobServer
import org.jobrunr.server.BackgroundJobServerConfiguration
import org.jobrunr.storage.InMemoryStorageProvider
import org.jobrunr.storage.PageRequest
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import java.util.concurrent.TimeUnit

class KotlinJobSchedulerTest {
  @Mock
  private val storageProvider = InMemoryStorageProvider().also {
    it.setJobMapper(JobMapper(JacksonJsonMapper()))
  }
  private val jobClientLogFilter = JobClientLogFilter()
  private val jobScheduler = JobScheduler(storageProvider, listOf(jobClientLogFilter))
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
    val jobId = jobScheduler.enqueue { "foo" }
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
    val jobId = jobScheduler.enqueue { "$text: $amount" }
    assertThat(jobClientLogFilter.onCreating).isTrue
    assertThat(jobClientLogFilter.onCreated).isTrue
    await().atMost(Durations.FIVE_SECONDS).until {
      storageProvider.getJobById(jobId).state == StateName.SUCCEEDED
    }
    assertThat(storageProvider.getJobById(jobId))
      .hasStates(StateName.ENQUEUED, StateName.PROCESSING, StateName.SUCCEEDED)
  }

  @Test
  fun `test schedule recurrent job`() {
    val amount = 2
    val text = "foo"
    jobScheduler.scheduleRecurrently(Cron.minutely()) { "$text: $amount" }
    assertThat(jobClientLogFilter.onCreating).isTrue
    assertThat(jobClientLogFilter.onCreated).isTrue
    await().atMost(65, TimeUnit.SECONDS).until {
      storageProvider.countJobs(StateName.SUCCEEDED) == 1L
    }
    val job = storageProvider.getJobs(StateName.SUCCEEDED, PageRequest.ascOnUpdatedAt(1000))[0]
    assertThat(storageProvider.getJobById(job.id)).hasStates(
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

  @Test
  fun `test enqueue lambda with method reference`() {
    val container = TestLambdaContainer()
    val jobId = jobScheduler.enqueue(container::lambdaMethod)
    assertThat(jobClientLogFilter.onCreating).isTrue
    assertThat(jobClientLogFilter.onCreated).isTrue
    await().atMost(Durations.FIVE_SECONDS).until {
      storageProvider.getJobById(jobId).state == StateName.SUCCEEDED
    }
    assertThat(storageProvider.getJobById(jobId))
      .hasStates(StateName.ENQUEUED, StateName.PROCESSING, StateName.SUCCEEDED)
  }
}