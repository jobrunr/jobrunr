package org.jobrunr.kotlin.serialization.jobs

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import org.jobrunr.jobs.Job
import org.jobrunr.jobs.JobDetails
import org.jobrunr.jobs.states.JobState
import org.jobrunr.kotlin.serialization.MetadataSerializer
import org.jobrunr.kotlin.serialization.misc.UUIDSerializer
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
object JobSerializer : KSerializer<Job> {
	private val jobHistorySerializer = ListSerializer(PolymorphicSerializer(JobState::class))
	
	override val descriptor = buildClassSerialDescriptor(Job::class.qualifiedName!!) {
		element("id", UUIDSerializer.descriptor)
		element("version", Int.serializer().descriptor)
		element("jobSignature", String.serializer().descriptor)
		element("jobName", String.serializer().descriptor)
		element("labels", ListSerializer(String.serializer()).descriptor)
		element("jobDetails", JobDetailsSerializer.descriptor)
		element("jobHistory", jobHistorySerializer.descriptor)
		element("metadata", MetadataSerializer.descriptor)
	}

	override fun serialize(encoder: Encoder, value: Job) = encoder.encodeStructure(descriptor) {
		encodeSerializableElement(descriptor, 0, UUIDSerializer, value.id)
		encodeIntElement(descriptor, 1, value.version)
		encodeStringElement(descriptor, 2, value.jobSignature)
		encodeStringElement(descriptor, 3, value.jobName)
		encodeSerializableElement(descriptor, 4, ListSerializer(String.serializer()), value.labels)
		encodeSerializableElement(descriptor, 5, JobDetailsSerializer, value.jobDetails)

		encodeSerializableElement(descriptor, 6, jobHistorySerializer, value.jobStates)

		encodeSerializableElement(descriptor, 7, MetadataSerializer, value.metadata)
	}

	override fun deserialize(decoder: Decoder): Job = decoder.decodeStructure(descriptor) {
		lateinit var id: UUID
		var version = -1
		lateinit var jobName: String
		lateinit var labels: List<String>
		lateinit var jobDetails: JobDetails
		lateinit var jobHistory: List<JobState>
		lateinit var metadata: Map<String, Any>

		while (true) {
			when (val index = decodeElementIndex(descriptor)) {
				CompositeDecoder.DECODE_DONE -> break
				0 -> id = decodeSerializableElement(descriptor, 0, UUIDSerializer)
				1 -> version = decodeIntElement(descriptor, 1)
				2 -> decodeStringElement(descriptor, 2)
				3 -> jobName = decodeStringElement(descriptor, 3)
				4 -> labels = decodeSerializableElement(descriptor, 4, ListSerializer(String.serializer()))
				5 -> jobDetails = decodeSerializableElement(descriptor, 5, JobDetailsSerializer)
				6 -> jobHistory = decodeSerializableElement(descriptor, 6, jobHistorySerializer)
				7 -> metadata = decodeSerializableElement(descriptor, 7, MetadataSerializer)
				else -> error("Unexpected index $index")
			}
		}

		Job(
			id,
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
