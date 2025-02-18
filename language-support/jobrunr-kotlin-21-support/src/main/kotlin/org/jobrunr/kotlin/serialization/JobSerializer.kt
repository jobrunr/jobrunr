package org.jobrunr.kotlin.serialization

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.*
import org.jobrunr.jobs.Job
import org.jobrunr.jobs.JobDetails
import org.jobrunr.jobs.states.JobState
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(InternalSerializationApi::class, ExperimentalUuidApi::class)
object JobSerializer : KSerializer<Job> {
	override val descriptor = buildClassSerialDescriptor(Job::class.qualifiedName!!) {
		element("id", Uuid.serializer().descriptor)
		element("version", Int.serializer().descriptor)
		element("jobSignature", String.serializer().descriptor)
		element("jobName", String.serializer().descriptor)
		element("labels", SetSerializer(String.serializer()).descriptor)
		element("jobDetails", JobDetailsSerializer.descriptor)
		element(
			"jobHistory", listSerialDescriptor(
				buildSerialDescriptor(JobState::class.qualifiedName!!, SerialKind.CONTEXTUAL)
			)
		)
		element("metadata", MetadataSerializer.descriptor)
	}

	override fun serialize(encoder: Encoder, value: Job) = encoder.encodeStructure(descriptor) {
		encodeSerializableElement(descriptor, 0, Uuid.serializer(), value.id.toKotlinUuid())
		encodeIntElement(descriptor, 1, value.version)
		encodeStringElement(descriptor, 2, value.jobSignature)
		encodeStringElement(descriptor, 3, value.jobName)
		encodeSerializableElement(descriptor, 4, SetSerializer(String.serializer()), value.labels)
		encodeSerializableElement(descriptor, 5, JobDetailsSerializer, value.jobDetails)

		encodeSerializableElement(descriptor, 6, ListSerializer(PolymorphicSerializer(JobState::class)), value.jobStates)

		encodeSerializableElement(descriptor, 7, MetadataSerializer, value.metadata)
	}

	override fun deserialize(decoder: Decoder): Job = decoder.decodeStructure(descriptor) {
		lateinit var id: Uuid
		var version = -1
		lateinit var jobName: String
		lateinit var labels: Set<String>
		lateinit var jobDetails: JobDetails
		lateinit var jobHistory: List<JobState>
		lateinit var metadata: Map<String, Any>

		while (true) {
			when (val index = decodeElementIndex(descriptor)) {
				0 -> id = decodeSerializableElement(descriptor, 0, Uuid.serializer())
				1 -> version = decodeIntElement(descriptor, 1)
				2 -> decodeStringElement(descriptor, 2)
				3 -> jobName = decodeStringElement(descriptor, 3)
				4 -> labels = decodeSerializableElement(descriptor, 4, SetSerializer(String.serializer()))
				5 -> jobDetails = decodeSerializableElement(descriptor, 5, JobDetailsSerializer)
				6 -> jobHistory = decodeSerializableElement(descriptor, 6, ListSerializer(PolymorphicSerializer(JobState::class)))
				7 -> metadata = decodeSerializableElement(descriptor, 7, MetadataSerializer)
				CompositeDecoder.DECODE_DONE -> break
				else -> error("Unexpected index: $index")
			}
		}

		Job(
			id.toJavaUuid(),
			version,
			jobDetails,
			jobHistory,
			ConcurrentHashMap(metadata),
		).apply {
			setRecurringJobId(recurringJobId.getOrNull())
			this.jobName = jobName
			this.labels = labels
		}
	}
}