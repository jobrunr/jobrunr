package org.jobrunr.kotlin.di

import org.jobrunr.server.JobActivator
import org.koin.core.component.KoinComponent

object KoinJobActivator : JobActivator, KoinComponent {
    override fun <T : Any> activateJob(type: Class<T>): T = getKoin().get(type.kotlin)
}
