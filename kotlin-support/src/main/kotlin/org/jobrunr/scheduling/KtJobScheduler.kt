package org.jobrunr.scheduling

import org.jobrunr.jobs.JobId
import org.jobrunr.jobs.lambdas.IocJobLambda
import org.jobrunr.jobs.lambdas.JobLambda
import java.time.*
import java.util.*

class KtJobScheduler(
        val jobScheduler: JobScheduler
) {
    /**
     * Creates a new fire-and-forget job based on a given function.
     * @param job the function which defines the fire-and-forget job
     * @return the id of the job
     */
    fun enqueue(job: JobLambda): JobId {
        return jobScheduler.enqueue(job)
    }

    /**
     * Creates a new fire-and-forget job based on a given function.
     * @param job the function which defines the fire-and-forget job
     * @return the id of the job
     */
    inline fun <reified S> enqueue(job: IocJobLambda<S>): JobId {
        return jobScheduler.enqueue(job)
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time..
     *
     * @param zonedDateTime The moment in time at which the job will be enqueued.
     * @param job           the function which defines the fire-and-forget job
     * @return the id of the Job
     */
    fun schedule(zonedDateTime: ZonedDateTime, job: JobLambda): JobId {
        return jobScheduler.schedule(job, zonedDateTime)
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time..
     *
     * @param offsetDateTime The moment in time at which the job will be enqueued.
     * @param job            the function which defines the fire-and-forget job
     * @return the id of the Job
     */
    fun schedule(offsetDateTime: OffsetDateTime, job: JobLambda): JobId {
        return jobScheduler.schedule(job, offsetDateTime)
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time..
     *
     * @param localDateTime The moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
     * @param job           the function which defines the fire-and-forget job
     * @return the id of the Job
     */
    fun schedule(localDateTime: LocalDateTime, job: JobLambda): JobId {
        return jobScheduler.schedule(job, localDateTime)
    }

    /**
     * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time..
     *
     * @param instant The moment in time at which the job will be enqueued.
     * @param job     the function which defines the fire-and-forget job
     * @return the id of the Job
     */
    fun schedule(instant: Instant, job: JobLambda): JobId {
        return jobScheduler.schedule(job, instant)
    }

    /**
     * Creates a new recurring job based on the given cron expression and the given lambda. The jobs will be scheduled using the systemDefault timezone.
     *
     * @param cron The cron expression defining when to run this recurring job
     * @param job  the function which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    fun scheduleRecurrently(cron: String, job: JobLambda): String {
        return jobScheduler.scheduleRecurrently(job, cron)
    }

    fun scheduleRecurrently(id: String, cron: String, job: JobLambda): String {
        return jobScheduler.scheduleRecurrently(id, job, cron)
    }

    fun scheduleRecurrently(id: String, cron: String, zoneId: ZoneId, job: JobLambda): String {
        return jobScheduler.scheduleRecurrently(id, job, cron, zoneId)
    }

    /**
     * Deletes a job and sets it's state to DELETED. If the job is being processed, it will be interrupted.
     *
     * @param id the id of the job
     */
    fun delete(id: UUID) {
        jobScheduler.delete(id)
    }

    fun delete(id: JobId) {
        jobScheduler.delete(id)
    }

    fun delete(id: String) {
        jobScheduler.delete(id)
    }
}