package org.jobrunr.scheduling

import org.jobrunr.storage.StorageProvider

object JobSchedulerExtensions {
  fun JobScheduler.useKotlin(storageProvider: StorageProvider): JobScheduler {
    KtBackgroundJob.jobScheduler = KtJobScheduler(storageProvider)
    return this
  }
}