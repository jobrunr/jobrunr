package org.jobrunr.scheduling

import org.jobrunr.jobs.*
import org.jobrunr.jobs.filters.JobDefaultFilters
import org.jobrunr.jobs.filters.JobFilter
import org.jobrunr.jobs.filters.JobFilterUtils
import org.jobrunr.jobs.states.ScheduledState
import org.jobrunr.scheduling.cron.CronExpression
import org.jobrunr.storage.StorageProvider
import java.time.*
import java.time.ZoneId.systemDefault
import java.util.*
import kotlin.jvm.internal.CallableReference

internal class KtJobScheduler(
  private val storageProvider: StorageProvider,
  private val jobFilterUtils: JobFilterUtils = JobFilterUtils(JobDefaultFilters(listOf()))
) {
  /**
   * Creates a new fire-and-forget job based on a given function.
   * @param lambda the function which defines the fire-and-forget job
   * @return the id of the job
   */
  fun enqueue(lambda: Function<*>): JobId {
    val jobDetails = lambdaToJobDetails(lambda)
    return saveJob(Job(jobDetails))
  }

  /**
   * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time..
   *
   * @param zonedDateTime The moment in time at which the job will be enqueued.
   * @param lambda        the function which defines the fire-and-forget job
   * @return the id of the Job
   */
  fun schedule(zonedDateTime: ZonedDateTime, lambda: Function<*>): JobId {
    val jobDetails = lambdaToJobDetails(lambda)
    return schedule(jobDetails, zonedDateTime.toInstant())
  }

  /**
   * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time..
   *
   * @param offsetDateTime The moment in time at which the job will be enqueued.
   * @param lambda        the function which defines the fire-and-forget job
   * @return the id of the Job
   */
  fun schedule(offsetDateTime: OffsetDateTime, lambda: Function<*>): JobId {
    val jobDetails = lambdaToJobDetails(lambda)
    return schedule(jobDetails, offsetDateTime.toInstant())
  }

  /**
   * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time..
   *
   * @param localDateTime The moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
   * @param lambda        the function which defines the fire-and-forget job
   * @return the id of the Job
   */
  fun schedule(localDateTime: LocalDateTime, lambda: Function<*>): JobId {
    val jobDetails = lambdaToJobDetails(lambda)
    return schedule(jobDetails, localDateTime.atZone(systemDefault()).toInstant())
  }

  /**
   * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time..
   *
   * @param instant The moment in time at which the job will be enqueued.
   * @param lambda        the function which defines the fire-and-forget job
   * @return the id of the Job
   */
  fun schedule(instant: Instant, lambda: Function<*>): JobId {
    val jobDetails = lambdaToJobDetails(lambda)
    return schedule(jobDetails, instant)
  }

  /**
   * Creates a new recurring job based on the given cron expression and the given lambda. The jobs will be scheduled using the systemDefault timezone.
   *
   * @param cron The cron expression defining when to run this recurring job
   * @param lambda  the function which defines the fire-and-forget job
   * @return the id of this recurring job which can be used to alter or delete it
   * @see org.jobrunr.scheduling.cron.Cron
   */
  fun scheduleRecurrently(cron: String, lambda: Function<*>): String {
    val zoneId = systemDefault()
    return scheduleRecurrently(cron, zoneId, lambda)
  }

  fun scheduleRecurrently(id: String, cron: String, lambda: Function<*>): String {
    val jobDetails = lambdaToJobDetails(lambda)
    val cronExpression = CronExpression.create(cron)
    return scheduleRecurrently(id, jobDetails, cronExpression, systemDefault())
  }

  fun scheduleRecurrently(id: String, cron: String, zoneId: ZoneId, lambda: Function<*>): String {
    val jobDetails = lambdaToJobDetails(lambda)
    val cronExpression = CronExpression.create(cron)
    return scheduleRecurrently(id, jobDetails, cronExpression, zoneId)
  }

  /**
  Creates a new recurring job based on the cron expression, {@code ZoneId} and lambda.
   *
   * @param cron The cron expression defining when to run this recurring job
   * @param lambda  the function which defines the fire-and-forget job
   * @return the id of this recurring job which can be used to alter or delete it
   * @see org.jobrunr.scheduling.cron.Cron
   */
  fun scheduleRecurrently(cron: String, zoneId: ZoneId, lambda: Function<*>): String {
    val jobDetails = lambdaToJobDetails(lambda)
    val cronExpression = CronExpression.create(cron)
    return scheduleRecurrently(null, jobDetails, cronExpression, zoneId)
  }

  /**
   * Deletes a job and sets it's state to DELETED. If the job is being processed, it will be interrupted.
   *
   * @param id the id of the job
   */
  fun delete(id: UUID) {
    storageProvider.delete(id)
  }

  fun delete(id: String) {
    storageProvider.deleteRecurringJob(id)
  }

  /**
   * @see .delete
   */
  fun delete(jobId: JobId) {
    storageProvider.delete(jobId.asUUID())
  }

  private fun saveJob(job: Job): JobId {
    jobFilterUtils.runOnCreatingFilter(job)
    val savedJob = storageProvider.save(job)
    jobFilterUtils.runOnCreatedFilter(savedJob)
    return JobId(savedJob.id)
  }

  fun scheduleRecurrently(
    id: String?,
    jobDetails: JobDetails,
    cronExpression: CronExpression,
    zoneId: ZoneId
  ): String {
    val recurringJob = RecurringJob(id, jobDetails, cronExpression, zoneId)
    jobFilterUtils.runOnCreatingFilter(recurringJob)
    val savedRecurringJob = storageProvider.saveRecurringJob(recurringJob)
    jobFilterUtils.runOnCreatedFilter(recurringJob)
    return savedRecurringJob.id
  }

  private fun schedule(jobDetails: JobDetails, scheduleAt: Instant): JobId {
    return saveJob(Job(jobDetails, ScheduledState(scheduleAt)))
  }

  private fun lambdaToJobDetails(lambda: Function<*>): JobDetails {
    val klass = if (lambda is CallableReference) lambda.boundReceiver.javaClass else lambda.javaClass
    val className = klass.name
    val constructor = klass.declaredConstructors.first()
    val boundVariables = if (lambda is CallableReference) {
      if (constructor.parameterCount == 0) listOf() else listOf(lambda.boundReceiver)
    } else {
      collectBoundVariables(lambda)
    }
    val jobParameters = boundVariables.map { JobParameter(it.javaClass, it) }
    return JobDetails(className, null, klass.methods[0].name, listOf(), jobParameters)
  }

  private fun <T> collectBoundVariables(lambda: Function<T>) =
    lambda.javaClass.declaredFields
      .filterNot { setOf("INSTANCE", "\$jacocoData").contains(it.name) }
      .map {
        it.isAccessible = true
        when (it.type) {
          Int::class.java -> it.getInt(lambda)
          Boolean::class.java -> it.getBoolean(lambda)
          Byte::class.java -> it.getByte(lambda)
          Char::class.java -> it.getChar(lambda)
          Double::class.java -> it.getDouble(lambda)
          Float::class.java -> it.getFloat(lambda)
          Long::class.java -> it.getLong(lambda)
          Short::class.java -> it.getShort(lambda)
          String::class.java -> it.get(lambda)
          else -> it.get(lambda)
        }
      }
}