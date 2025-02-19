package org.jobrunr.kotlin.serialization.storage

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import org.jobrunr.kotlin.serialization.misc.InstantSerializer
import org.jobrunr.storage.JobStatsExtended

object JobStatsExtendedSerializer : KSerializer<JobStatsExtended> {
	override val descriptor = buildClassSerialDescriptor(JobStatsExtended::class.qualifiedName!!) {
		JobStatsSerializer.buildClassSerialDescriptorElements(this)
		element("amountSucceeded", Long.serializer().descriptor)
		element("amountFailed", Long.serializer().descriptor)
		element("estimation", EstimationSerializer.descriptor)
	}

	override fun serialize(encoder: Encoder, value: JobStatsExtended) = encoder.encodeStructure(descriptor) {
		with (JobStatsSerializer) {
			continueEncode(value)
		}
		encodeLongElement(descriptor, descriptor.getElementIndex("amountSucceeded"), value.amountSucceeded)
		encodeLongElement(descriptor, descriptor.getElementIndex("amountFailed"), value.amountFailed)
		encodeSerializableElement(descriptor, descriptor.getElementIndex("estimation"), EstimationSerializer, value.estimation)
	}

	override fun deserialize(decoder: Decoder) = TODO("Not yet implemented")
	
	@OptIn(ExperimentalSerializationApi::class)
	object EstimationSerializer : KSerializer<JobStatsExtended.Estimation> {
		override val descriptor = buildClassSerialDescriptor(JobStatsExtended.Estimation::class.qualifiedName!!) {
			element("processingDone", Boolean.serializer().descriptor)
			element("estimatedProcessingTimeAvailable", Boolean.serializer().descriptor)
			element("estimatedProcessingFinishedAt", InstantSerializer.descriptor)
		}

		override fun serialize(encoder: Encoder, value: JobStatsExtended.Estimation) = encoder.encodeStructure(descriptor) {
			encodeBooleanElement(descriptor, 0, value.isProcessingDone)
			encodeBooleanElement(descriptor, 1, value.isEstimatedProcessingFinishedInstantAvailable)
			encodeNullableSerializableElement(descriptor, 2, InstantSerializer, value.estimatedProcessingFinishedAt)
		}

		override fun deserialize(decoder: Decoder) = TODO("Not yet implemented")
	}
}