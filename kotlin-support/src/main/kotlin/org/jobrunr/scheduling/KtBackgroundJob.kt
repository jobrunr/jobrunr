package org.jobrunr.scheduling

import org.jobrunr.jobs.JobId
import org.jobrunr.jobs.lambdas.IocJobLambda
import org.jobrunr.jobs.lambdas.IocJobLambdaFromStream
import org.jobrunr.jobs.lambdas.JobLambda
import org.jobrunr.jobs.lambdas.JobLambdaFromStream
import java.time.*
import java.util.*
import java.util.stream.Stream

object KtBackgroundJob {
  var jobScheduler: JobScheduler? = null

  /**
   * Creates a new fire-and-forget job based on a given lambda.
   * <h5>An example:</h5>
   * <pre>`MyService service = new MyService();
   * enqueue(() -> service.doWork());
  `</pre> *
   *
   * @param job the lambda which defines the fire-and-forget job
   * @return the id of the job
   */
  fun enqueue(job: JobLambda): JobId? {
    verifyJobScheduler()
    return jobScheduler!!.enqueue(job)
  }

  /**
   * Creates new fire-and-forget jobs for each item in the input stream using the lambda passed as `jobFromStream`.
   * <h5>An example:</h5>
   * <pre>`MyService service = new MyService();
   * Stream<UUID> workStream = getWorkStream();
   * enqueue(workStream, (uuid) -> service.doWork(uuid));
  `</pre> *
   *
   * @param input         the stream of items for which to create fire-and-forget jobs
   * @param jobFromStream the lambda which defines the fire-and-forget job to create for each item in the `input`
   */
  fun <T> enqueue(input: Stream<T>, jobFromStream: JobLambdaFromStream<T>) {
    verifyJobScheduler()
    jobScheduler!!.enqueue(input, jobFromStream)
  }

  /**
   * Creates a new fire-and-forget job based on a given lambda. The IoC container will be used to resolve `MyService`.
   * <h5>An example:</h5>
   * <pre>`<MyService>enqueue(x -> x.doWork());
  `</pre> *
   *
   * @param iocJob the lambda which defines the fire-and-forget job
   * @return the id of the job
   */
  fun <S> enqueue(iocJob: IocJobLambda<S>): JobId? {
    verifyJobScheduler()
    return jobScheduler!!.enqueue(iocJob)
  }

  /**
   * Creates new fire-and-forget jobs for each item in the input stream using the lambda passed as `jobFromStream`. The IoC container will be used to resolve `MyService`.
   * <h5>An example:</h5>
   * <pre>`Stream<UUID> workStream = getWorkStream();
   * <MyService, UUID>enqueue(workStream, (x, uuid) -> x.doWork(uuid));
  `</pre> *
   *
   * @param input            the stream of items for which to create fire-and-forget jobs
   * @param iocJobFromStream the lambda which defines the fire-and-forget job to create for each item in the `input`
   */
  fun <S, T> enqueue(input: Stream<T>, iocJobFromStream: IocJobLambdaFromStream<S, T>) {
    verifyJobScheduler()
    jobScheduler!!.enqueue(input, iocJobFromStream)
  }

  /**
   * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
   * <h5>An example:</h5>
   * <pre>`MyService service = new MyService();
   * schedule(() -> service.doWork(), ZonedDateTime.now().plusHours(5));
  `</pre> *
   *
   * @param job           the lambda which defines the fire-and-forget job
   * @param zonedDateTime The moment in time at which the job will be enqueued.
   * @return the id of the Job
   */
  fun schedule(job: JobLambda, zonedDateTime: ZonedDateTime): JobId? {
    verifyJobScheduler()
    return jobScheduler!!.schedule(job, zonedDateTime)
  }

  /**
   * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve `MyService`.
   * <h5>An example:</h5>
   * <pre>`<MyService>schedule(x -> x.doWork(), ZonedDateTime.now().plusHours(5));
  `</pre> *
   *
   * @param iocJob        the lambda which defines the fire-and-forget job
   * @param zonedDateTime The moment in time at which the job will be enqueued.
   * @return the id of the Job
   */
  fun <S> schedule(iocJob: IocJobLambda<S>, zonedDateTime: ZonedDateTime): JobId? {
    verifyJobScheduler()
    return jobScheduler!!.schedule(iocJob, zonedDateTime)
  }

  /**
   * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
   * <h5>An example:</h5>
   * <pre>`MyService service = new MyService();
   * schedule(() -> service.doWork(), OffsetDateTime.now().plusHours(5));
  `</pre> *
   *
   * @param job            the lambda which defines the fire-and-forget job
   * @param offsetDateTime The moment in time at which the job will be enqueued.
   * @return the id of the Job
   */
  fun schedule(job: JobLambda, offsetDateTime: OffsetDateTime): JobId? {
    verifyJobScheduler()
    return jobScheduler!!.schedule(job, offsetDateTime)
  }

  /**
   * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve `MyService`.
   * <h5>An example:</h5>
   * <pre>`<MyService>schedule(x -> x.doWork(), OffsetDateTime.now().plusHours(5));
  `</pre> *
   *
   * @param iocJob         the lambda which defines the fire-and-forget job
   * @param offsetDateTime The moment in time at which the job will be enqueued.
   * @return the id of the Job
   */
  fun <S> schedule(iocJob: IocJobLambda<S>, offsetDateTime: OffsetDateTime): JobId? {
    verifyJobScheduler()
    return jobScheduler!!.schedule(iocJob, offsetDateTime)
  }

  /**
   * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
   * <h5>An example:</h5>
   * <pre>`MyService service = new MyService();
   * schedule(() -> service.doWork(), LocalDateTime.now().plusHours(5));
  `</pre> *
   *
   * @param job           the lambda which defines the fire-and-forget job
   * @param localDateTime The moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
   * @return the id of the Job
   */
  fun schedule(job: JobLambda, localDateTime: LocalDateTime): JobId? {
    verifyJobScheduler()
    return jobScheduler!!.schedule(job, localDateTime)
  }

  /**
   * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve `MyService`.
   * <h5>An example:</h5>
   * <pre>`<MyService>schedule(x -> x.doWork(), LocalDateTime.now().plusHours(5));
  `</pre> *
   *
   * @param iocJob        the lambda which defines the fire-and-forget job
   * @param localDateTime The moment in time at which the job will be enqueued. It will use the systemDefault ZoneId to transform it to an UTC Instant
   * @return the id of the Job
   */
  fun <S> schedule(iocJob: IocJobLambda<S>, localDateTime: LocalDateTime): JobId? {
    verifyJobScheduler()
    return jobScheduler!!.schedule(iocJob, localDateTime)
  }

  /**
   * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time.
   * <h5>An example:</h5>
   * <pre>`MyService service = new MyService();
   * schedule(() -> service.doWork(), Instant.now().plusHours(5));
  `</pre> *
   *
   * @param job     the lambda which defines the fire-and-forget job
   * @param instant The moment in time at which the job will be enqueued.
   * @return the id of the Job
   */
  fun schedule(job: JobLambda, instant: Instant): JobId? {
    verifyJobScheduler()
    return jobScheduler!!.schedule(job, instant)
  }

  /**
   * Creates a new fire-and-forget job based on the given lambda and schedules it to be enqueued at the given moment of time. The IoC container will be used to resolve `MyService`.
   * <h5>An example:</h5>
   * <pre>`<MyService>schedule(x -> x.doWork(), Instant.now().plusHours(5));
  `</pre> *
   *
   * @param iocJob  the lambda which defines the fire-and-forget job
   * @param instant The moment in time at which the job will be enqueued.
   * @return the id of the Job
   */
  fun <S> schedule(iocJob: IocJobLambda<S>, instant: Instant): JobId? {
    verifyJobScheduler()
    return jobScheduler!!.schedule(iocJob, instant)
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
   * Creates a new recurring job based on the given lambda and the given cron expression. The jobs will be scheduled using the systemDefault timezone.
   * <h5>An example:</h5>
   * <pre>`MyService service = new MyService();
   * scheduleRecurrently(() -> service.doWork(), Cron.daily());
  `</pre> *
   *
   * @param job  the lambda which defines the fire-and-forget job
   * @param cron The cron expression defining when to run this recurring job
   * @return the id of this recurring job which can be used to alter or delete it
   * @see org.jobrunr.scheduling.cron.Cron
   */
  fun scheduleRecurrently(job: JobLambda, cron: String): String? {
    verifyJobScheduler()
    return jobScheduler!!.scheduleRecurrently(job, cron)
  }

  /**
   * Creates a new recurring job based on the given lambda and the given cron expression. The IoC container will be used to resolve `MyService`. The jobs will be scheduled using the systemDefault timezone.
   * <h5>An example:</h5>
   * <pre>`<MyService>scheduleRecurrently(x -> x.doWork(), Cron.daily());
  `</pre> *
   *
   * @param iocJob the lambda which defines the fire-and-forget job
   * @param cron   The cron expression defining when to run this recurring job
   * @return the id of this recurring job which can be used to alter or delete it
   * @see org.jobrunr.scheduling.cron.Cron
   */
  fun <S> scheduleRecurrently(iocJob: IocJobLambda<S>, cron: String): String? {
    verifyJobScheduler()
    return jobScheduler!!.scheduleRecurrently(iocJob, cron)
  }

  /**
   * Creates a new recurring job based on the given id, lambda and cron expression. The jobs will be scheduled using the systemDefault timezone
   * <h5>An example:</h5>
   * <pre>`MyService service = new MyService();
   * scheduleRecurrently("my-recurring-job", () -> service.doWork(), Cron.daily());
  `</pre> *
   *
   * @param id   the id of this recurring job which can be used to alter or delete it
   * @param job  the lambda which defines the fire-and-forget job
   * @param cron The cron expression defining when to run this recurring job
   * @return the id of this recurring job which can be used to alter or delete it
   * @see org.jobrunr.scheduling.cron.Cron
   */
  fun scheduleRecurrently(id: String, job: JobLambda?, cron: String): String? {
    verifyJobScheduler()
    return jobScheduler!!.scheduleRecurrently(id, job, cron)
  }

  /**
   * Creates a new recurring job based on the given id, lambda and cron expression. The IoC container will be used to resolve `MyService`. The jobs will be scheduled using the systemDefault timezone
   * <h5>An example:</h5>
   * <pre>`<MyService>scheduleRecurrently("my-recurring-job", x -> x.doWork(), Cron.daily());
  `</pre> *
   *
   * @param id     the id of this recurring job which can be used to alter or delete it
   * @param iocJob the lambda which defines the fire-and-forget job
   * @param cron   The cron expression defining when to run this recurring job
   * @return the id of this recurring job which can be used to alter or delete it
   * @see org.jobrunr.scheduling.cron.Cron
   */
  fun <S> scheduleRecurrently(id: String, iocJob: IocJobLambda<S>?, cron: String): String? {
    verifyJobScheduler()
    return jobScheduler!!.scheduleRecurrently(id, iocJob, cron)
  }

  /**
   * Creates a new recurring job based on the given id, lambda, cron expression and `ZoneId`.
   * <h5>An example:</h5>
   * <pre>`MyService service = new MyService();
   * scheduleRecurrently("my-recurring-job", () -> service.doWork(), Cron.daily(), ZoneId.of("Europe/Brussels"));
  `</pre> *
   *
   * @param id     the id of this recurring job which can be used to alter or delete it
   * @param job    the lambda which defines the fire-and-forget job
   * @param cron   The cron expression defining when to run this recurring job
   * @param zoneId The zoneId (timezone) of when to run this recurring job
   * @return the id of this recurring job which can be used to alter or delete it
   * @see org.jobrunr.scheduling.cron.Cron
   */
  fun scheduleRecurrently(id: String, job: JobLambda, cron: String, zoneId: ZoneId): String? {
    verifyJobScheduler()
    return jobScheduler!!.scheduleRecurrently(id, job, cron, zoneId)
  }

  /**
   * Creates a new recurring job based on the given id, lambda, cron expression and `ZoneId`. The IoC container will be used to resolve `MyService`.
   * <h5>An example:</h5>
   * <pre>`<MyService>scheduleRecurrently("my-recurring-job", x -> x.doWork(), Cron.daily(), ZoneId.of("Europe/Brussels"));
  `</pre> *
   *
   * @param id     the id of this recurring job which can be used to alter or delete it
   * @param iocJob the lambda which defines the fire-and-forget job
   * @param cron   The cron expression defining when to run this recurring job
   * @param zoneId The zoneId (timezone) of when to run this recurring job
   * @return the id of this recurring job which can be used to alter or delete it
   * @see org.jobrunr.scheduling.cron.Cron
   */
  fun <S> scheduleRecurrently(
    id: String,
    iocJob: IocJobLambda<S>,
    cron: String,
    zoneId: ZoneId
  ): String? {
    verifyJobScheduler()
    return jobScheduler!!.scheduleRecurrently(id, iocJob, cron, zoneId)
  }

  /**
   * Deletes the recurring job based on the given id.
   * <h5>An example:</h5>
   * <pre>`delete("my-recurring-job"));
  `</pre> *
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