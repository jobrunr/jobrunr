package org.jobrunr.dashboard

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import org.jobrunr.kotlin.utils.mapper.KotlinxSerializationJsonMapper

internal class KotlinxSerializationDashboardWebServerTest : JobRunrDashboardWebServerTest() {
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override fun getJsonMapper() = KotlinxSerializationJsonMapper()
}