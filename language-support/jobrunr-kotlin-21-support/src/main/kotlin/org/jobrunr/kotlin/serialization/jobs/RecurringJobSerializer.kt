package org.jobrunr.kotlin.serialization.jobs

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.ClassSerialDescriptorBuilder
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.*
import org.jobrunr.jobs.JobDetails
import org.jobrunr.jobs.RecurringJob
import org.jobrunr.kotlin.serialization.utils.ClassDiscriminatedContextualSerializer

@OptIn(ExperimentalSerializationApi::class)
object RecurringJobSerializer : KSerializer<RecurringJob>, ClassDiscriminatedContextualSerializer.PolymorphicContinuationSerializer<RecurringJob> {
	@OptIn(InternalSerializationApi::class)
	val createdByDescriptor = buildSerialDescriptor(RecurringJob.CreatedBy ::class.qualifiedName!!, SerialKind.ENUM)

	val buildClassSerialDescriptorElements = fun ClassSerialDescriptorBuilder.() {
		element("id", String.serializer().descriptor)
		element("version", Int.serializer().descriptor)
		element("jobName", String.serializer().descriptor)
		element("amountOfRetries", Int.serializer().descriptor)
		element("labels", SetSerializer(String.serializer()).descriptor)
		element("jobDetails", JobDetailsSerializer.descriptor)
		element("scheduleExpression", String.serializer().descriptor)
		element("zoneId", String.serializer().descriptor)
		element("createdBy", createdByDescriptor)
		element("createdAt", String.serializer().descriptor)
	}
	
	override val descriptor = buildClassSerialDescriptor(RecurringJob::class.qualifiedName!!, builderAction = buildClassSerialDescriptorElements)

	override fun serialize(encoder: Encoder, value: RecurringJob) = encoder.encodeStructure(descriptor) {
		continueEncode(value)
	}

	override fun CompositeEncoder.continueEncode(value: RecurringJob) {
		encodeStringElement(descriptor, 0, value.id)
		encodeIntElement(descriptor, 1, value.version)
		encodeStringElement(descriptor, 2, value.jobName)
		encodeNullableSerializableElement(descriptor, 3, Int.serializer(), value.amountOfRetries)
		encodeSerializableElement(descriptor, 4, ListSerializer(String.serializer()), value.labels)
		encodeSerializableElement(descriptor, 5, JobDetailsSerializer, value.jobDetails)
		encodeStringElement(descriptor, 6, value.scheduleExpression)
		encodeStringElement(descriptor, 7, value.zoneId)
		encodeStringElement(descriptor, 8, value.createdBy.name)
		encodeStringElement(descriptor, 9, value.createdAt.toString())
	}

	override fun deserialize(decoder: Decoder): RecurringJob = decoder.decodeStructure(descriptor) {
		lateinit var id: String
		var version = -1
		lateinit var jobName: String
		var amountOfRetries: Int? = null
		lateinit var labels: List<String>
		lateinit var jobDetails: JobDetails
		lateinit var scheduleExpression: String
		lateinit var zoneId: String
		lateinit var createdBy: RecurringJob.CreatedBy
		lateinit var createdAt: String

		while (true) {
			when (val index = decodeElementIndex(descriptor)) {
				CompositeDecoder.DECODE_DONE -> break
				0 -> id = decodeStringElement(descriptor, 0)
				1 -> version = decodeIntElement(descriptor, 1)
				2 -> jobName = decodeStringElement(descriptor, 2)
				3 -> amountOfRetries = decodeNullableSerializableElement(descriptor, 3, Int.serializer())
				4 -> labels = decodeSerializableElement(descriptor, 4, ListSerializer(String.serializer()))
				5 -> jobDetails = decodeSerializableElement(descriptor, 5, JobDetailsSerializer)
				6 -> scheduleExpression = decodeStringElement(descriptor, 6)
				7 -> zoneId = decodeStringElement(descriptor, 7)
				8 -> createdBy = RecurringJob.CreatedBy.valueOf(decodeStringElement(createdByDescriptor, 8))
				9 -> createdAt = decodeStringElement(descriptor, 9)
				else -> error("Unexpected index $index")
			}
		}

		RecurringJob(
			id,
			version,
			jobDetails,
			scheduleExpression,
			zoneId,
			createdBy,
			createdAt
		).apply {
			this.jobName = jobName
			this.amountOfRetries = amountOfRetries
			this.labels = labels
		}
	}
}
