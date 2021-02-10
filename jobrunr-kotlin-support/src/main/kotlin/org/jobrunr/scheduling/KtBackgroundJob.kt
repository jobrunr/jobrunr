package org.jobrunr.scheduling

import org.jobrunr.jobs.JobId
import org.jobrunr.jobs.lambdas.JobLambda
import java.time.*
import java.util.*

object KtBackgroundJob {
    internal var jobScheduler: KtJobScheduler? = null

    /**
     * Creates a new fire-and-forget job based on a given function.
     * @param job the function which defines the fire-and-forget job
     * @return the id of the job
     */
    fun enqueue(job: JobLambda): JobId {
        verifyJobScheduler()
        return jobScheduler!!.enqueue(job)
    }

    /**
     * Creates a new fire-and-forget job based on the given function and schedules it to be enqueued at the given moment of time.
     *
     * @param zonedDateTime The moment in time at which the job will be enqueued.
     * @param job           the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    fun schedule(zonedDateTime: ZonedDateTime, job: JobLambda): JobId {
        verifyJobScheduler()
        return jobScheduler!!.schedule(zonedDateTime, job)
    }

    /**
     * Creates a new fire-and-forget job based on the given function and schedules it to be enqueued at the given moment of time.
     *
     * @param offsetDateTime The moment in time at which the job will be enqueued.
     * @param job            the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    fun schedule(offsetDateTime: OffsetDateTime, job: JobLambda): JobId {
        verifyJobScheduler()
        return jobScheduler!!.schedule(offsetDateTime, job)
    }

    /**
     * Creates a new fire-and-forget job based on the given function and schedules it to be enqueued at the given moment of time.
     *
     * @param localDateTime The moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
     * @param job           the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    fun schedule(localDateTime: LocalDateTime, job: JobLambda): JobId {
        verifyJobScheduler()
        return jobScheduler!!.schedule(localDateTime, job)
    }

    /**
     * Creates a new fire-and-forget job based on the given function and schedules it to be enqueued at the given moment of time.
     *
     * @param instant The moment in time at which the job will be enqueued.
     * @param job     the lambda which defines the fire-and-forget job
     * @return the id of the Job
     */
    fun schedule(instant: Instant, job: JobLambda): JobId {
        verifyJobScheduler()
        return jobScheduler!!.schedule(instant, job)
    }

    /**
     * Deletes a job and sets it's state to DELETED. If the job is being processed, it will be interrupted.
     *
     * @param id the id of the job
     */
    fun delete(id: UUID) {
        verifyJobScheduler()
        jobScheduler!!.delete(id)
    }

    /**
     * @see .delete
     */
    fun delete(jobId: JobId) {
        delete(jobId.asUUID())
    }

    /**
     * Creates a new recurring job based on the given function and the given cron expression. The jobs will be scheduled using the systemDefault timezone.
     *
     * @param cron The cron expression defining when to run this recurring job
     * @param job  the lambda which defines the fire-and-forget job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    fun scheduleRecurrently(cron: String, job: JobLambda): String {
        verifyJobScheduler()
        return jobScheduler!!.scheduleRecurrently(cron, job)
    }

    /**
     * Creates a new recurring job based on the given id, function and cron expression. The jobs will be scheduled using the systemDefault timezone
     *
     * @param id   the id of this recurring job which can be used to alter or delete it
     * @param job  the lambda which defines the fire-and-forget job
     * @param cron The cron expression defining when to run this recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    fun scheduleRecurrently(id: String, cron: String, job: JobLambda): String {
        verifyJobScheduler()
        return jobScheduler!!.scheduleRecurrently(id, cron, job)
    }

    /**
     * Creates a new recurring job based on the given id, function, cron expression and `ZoneId`.
     *
     * @param id     the id of this recurring job which can be used to alter or delete it
     * @param job    the lambda which defines the fire-and-forget job
     * @param cron   The cron expression defining when to run this recurring job
     * @param zoneId The zoneId (timezone) of when to run this recurring job
     * @return the id of this recurring job which can be used to alter or delete it
     * @see org.jobrunr.scheduling.cron.Cron
     */
    fun scheduleRecurrently(id: String, cron: String, zoneId: ZoneId, job: JobLambda): String {
        verifyJobScheduler()
        return jobScheduler!!.scheduleRecurrently(id, cron, zoneId, job)
    }

    /**
     * Deletes the recurring job based on the given id.
     *
     * @param id the id of the recurring job to delete
     */
    fun delete(id: String) {
        verifyJobScheduler()
        jobScheduler!!.delete(id)
    }

    private fun verifyJobScheduler() {
        if (jobScheduler != null) return
        throw IllegalStateException(
                "JobScheduler has not been initialized. Set it via `KtBackgroundJob.jobScheduler`.")
    }
}