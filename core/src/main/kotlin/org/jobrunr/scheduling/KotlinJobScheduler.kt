package org.jobrunr.scheduling

import org.jobrunr.jobs.Job
import org.jobrunr.jobs.JobDetails
import org.jobrunr.jobs.JobId
import org.jobrunr.jobs.JobParameter

class KotlinJobScheduler(private val jobScheduler: JobScheduler) {
  fun enqueue(lambda: Function<*>): JobId? {
    val klass = lambda.javaClass
    val className = klass.name
    val boundVariables = collectBoundVariables(lambda).map {
      JobParameter(it.javaClass, it)
    }
    val job = Job(JobDetails(className, null, klass.methods[0].name, listOf(), boundVariables))
    return jobScheduler.saveJob(job)
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