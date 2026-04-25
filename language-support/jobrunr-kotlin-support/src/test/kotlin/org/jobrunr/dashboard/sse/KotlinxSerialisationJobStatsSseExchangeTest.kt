package org.jobrunr.dashboard.sse

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import org.jobrunr.kotlin.utils.mapper.KotlinxSerializationJsonMapper
import org.jobrunr.utils.mapper.JsonMapper

class KotlinxSerialisationJobStatsSseExchangeTest : AbstractJobStatsSseExchangeTest() {
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override fun jsonMapper(): JsonMapper = KotlinxSerializationJsonMapper()
}