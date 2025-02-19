package org.jobrunr.kotlin.serialization.storage

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import org.jobrunr.kotlin.serialization.misc.InstantSerializer
import org.jobrunr.storage.JobRunrMetadata

object JobRunrMetadataSerializer : KSerializer<JobRunrMetadata> {
	override val descriptor = buildClassSerialDescriptor(JobRunrMetadata::class.qualifiedName!!) {
		element("name", String.serializer().descriptor)
		element("owner", String.serializer().descriptor)
		element("createdAt", InstantSerializer.descriptor)
		element("updatedAt", InstantSerializer.descriptor)
		element("value", String.serializer().descriptor)
	}

	override fun serialize(encoder: Encoder, value: JobRunrMetadata) = encoder.encodeStructure(descriptor) {
		encodeStringElement(descriptor, 0, value.name)
		encodeStringElement(descriptor, 1, value.owner)
		encodeSerializableElement(descriptor, 2, InstantSerializer, value.createdAt)
		encodeSerializableElement(descriptor, 3, InstantSerializer, value.updatedAt)
		encodeStringElement(descriptor, 4, value.value)
	}

	override fun deserialize(decoder: Decoder) = TODO("Not yet implemented")
}