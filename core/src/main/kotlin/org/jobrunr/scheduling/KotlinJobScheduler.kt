package org.jobrunr.scheduling

import org.jobrunr.jobs.Job
import org.jobrunr.jobs.JobDetails
import org.jobrunr.jobs.JobId
import org.jobrunr.jobs.JobParameter
import org.jobrunr.scheduling.cron.CronExpression
import java.time.ZoneId
import kotlin.jvm.internal.CallableReference

internal class KotlinJobScheduler(private val jobScheduler: JobScheduler) {
  /** TODO docs */
  fun enqueue(lambda: Function<*>): JobId {
    val jobDetails = lambdaToJobDetails(lambda)
    return jobScheduler.saveJob(Job(jobDetails))
  }

  /** TODO docs */
  fun scheduleRecurrently(lambda: Function<*>, cron: String): String {
    val jobDetails = lambdaToJobDetails(lambda)
    val cronExpression = CronExpression.create(cron)
    val zoneId = ZoneId.systemDefault()
    return jobScheduler.scheduleRecurrently(null, jobDetails, cronExpression, zoneId)
  }

  private fun lambdaToJobDetails(lambda: Function<*>): JobDetails {
    val klass = lambda.javaClass
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