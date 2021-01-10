package org.jobrunr.scheduling

object JobSchedulerExtensions {
  fun JobScheduler.useKotlin(): JobScheduler {
    KtBackgroundJob.jobScheduler = this
    return this
  }
}