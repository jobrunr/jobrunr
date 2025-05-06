package org.jobrunr.jobs.mappers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import org.jobrunr.kotlin.utils.mapper.KotlinxSerializationJsonMapper
import org.jobrunr.utils.mapper.AbstractJsonMapperTest
import org.junit.jupiter.api.Test

class KotlinxSerializationJsonMapperTest : AbstractJsonMapperTest() {
	@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
	override fun newJsonMapper() = KotlinxSerializationJsonMapper(testModule)

	@Test
	override fun testSerializeAndDeserializeEnqueuedJobComingFrom4Dot0Dot0() {
		// silenced because the v4.0.0 is too old and no longer supported
		// we may safely assume nobody is going to migrate straight to kotlinx serialization from that version
	}
}