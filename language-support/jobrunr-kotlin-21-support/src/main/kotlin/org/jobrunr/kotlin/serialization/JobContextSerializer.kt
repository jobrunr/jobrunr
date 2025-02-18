package org.jobrunr.kotlin.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import org.jobrunr.jobs.context.JobContext

object JobContextSerializer : KSerializer<JobContext> {
	override val descriptor = buildClassSerialDescriptor("org.jobrunr.jobs.context.JobContext")

	override fun serialize(encoder: Encoder, value: JobContext) = encoder.encodeStructure(descriptor) {}

	override fun deserialize(decoder: Decoder): JobContext = decoder.decodeStructure(descriptor) {
		JobContext.Null
	}
}