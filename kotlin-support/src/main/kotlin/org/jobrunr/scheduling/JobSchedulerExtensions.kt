package org.jobrunr.scheduling

object JobSchedulerExtensions {
    fun JobScheduler.useKotlin(): JobScheduler {
        KtBackgroundJob.jobScheduler = KtJobScheduler(this)
        return this
    }
}