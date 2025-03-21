package org.jobrunr.jobs.mappers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import org.jobrunr.kotlin.utils.mapper.KotlinxSerializationJsonMapper
import org.jobrunr.utils.mapper.AbstractJsonMapperTest

class KotlinxSerializationJsonMapperTest : AbstractJsonMapperTest() {
	@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
	override fun newJsonMapper() = KotlinxSerializationJsonMapper(testModule)
}