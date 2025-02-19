package org.jobrunr.kotlin.serialization.storage

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.ClassSerialDescriptorBuilder
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import org.jobrunr.kotlin.serialization.utils.ClassDiscriminatedContextualSerializer
import org.jobrunr.kotlin.serialization.misc.InstantSerializer
import org.jobrunr.storage.JobStats

object JobStatsSerializer : KSerializer<JobStats>, ClassDiscriminatedContextualSerializer.PolymorphicContinuationSerializer<JobStats> {
	val buildClassSerialDescriptorElements = fun ClassSerialDescriptorBuilder.() {
		element("timeStamp", InstantSerializer.descriptor)
		element("queryDurationInMillis", Long.serializer().descriptor)
		element("total", Long.serializer().descriptor)
		element("scheduled", Long.serializer().descriptor)
		element("enqueued", Long.serializer().descriptor)
		element("processing", Long.serializer().descriptor)
		element("failed", Long.serializer().descriptor)
		element("succeeded", Long.serializer().descriptor)
		element("allTimeSucceeded", Long.serializer().descriptor)
		element("deleted", Long.serializer().descriptor)
		element("recurringJobs", Int.serializer().descriptor)
		element("backgroundJobServers", Int.serializer().descriptor)
	}
	
	override val descriptor = buildClassSerialDescriptor(JobStats::class.qualifiedName!!, builderAction = buildClassSerialDescriptorElements)

	override fun serialize(encoder: Encoder, value: JobStats) = encoder.encodeStructure(descriptor) {
		continueEncode(value)
	}

	override fun CompositeEncoder.continueEncode(value: JobStats) {
		encodeSerializableElement(descriptor, 0, InstantSerializer, value.timeStamp)
		encodeLongElement(descriptor, 1, value.queryDurationInMillis)
		encodeLongElement(descriptor, 2, value.total)
		encodeLongElement(descriptor, 3, value.scheduled)
		encodeLongElement(descriptor, 4, value.enqueued)
		encodeLongElement(descriptor, 5, value.processing)
		encodeLongElement(descriptor, 6, value.failed)
		encodeLongElement(descriptor, 7, value.succeeded)
		encodeLongElement(descriptor, 8, value.allTimeSucceeded)
		encodeLongElement(descriptor, 9, value.deleted)
		encodeIntElement(descriptor, 10, value.recurringJobs)
		encodeIntElement(descriptor, 11, value.backgroundJobServers)
	}

	override fun deserialize(decoder: Decoder) = TODO("Not yet implemented")
}