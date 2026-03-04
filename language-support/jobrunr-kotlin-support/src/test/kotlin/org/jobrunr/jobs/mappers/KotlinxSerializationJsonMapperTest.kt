package org.jobrunr.jobs.mappers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import org.jobrunr.kotlin.utils.mapper.KotlinxSerializationJsonMapper
import org.jobrunr.utils.mapper.AbstractJsonMapperTest
import org.junit.jupiter.api.Disabled

class KotlinxSerializationJsonMapperTest : AbstractJsonMapperTest() {
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override fun newJsonMapper() = KotlinxSerializationJsonMapper(testModule)

    @Disabled("No regression introduced for kotlinx.serialization coming from 4.0.0, the support is introduced in v8")
    override fun testSerializeAndDeserializeEnqueuedJobComingFrom4Dot0Dot0() {
        // we may safely assume nobody is going to migrate straight to kotlinx serialization from JobRunr v4
    }
}