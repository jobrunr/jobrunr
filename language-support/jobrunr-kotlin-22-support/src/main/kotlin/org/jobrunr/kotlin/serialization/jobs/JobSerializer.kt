package org.jobrunr.kotlin.serialization.jobs

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import org.jobrunr.jobs.Job
import org.jobrunr.jobs.JobDetails
import org.jobrunr.jobs.states.JobState
import org.jobrunr.kotlin.serialization.misc.UUIDSerializer
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull

@OptIn(ExperimentalSerializationApi::class)
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
        element("metadata", JobMetadataSerializer.descriptor)
        element("amountOfRetries", Int.serializer().descriptor)
        element("recurringJobId", String.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: Job) = encoder.encodeStructure(descriptor) {
        encodeSerializableElement(descriptor, 0, UUIDSerializer, value.id)
        encodeIntElement(descriptor, 1, value.version)
        encodeStringElement(descriptor, 2, value.jobSignature)
        encodeStringElement(descriptor, 3, value.jobName)
        encodeSerializableElement(descriptor, 4, ListSerializer(String.serializer()), value.labels)
        encodeSerializableElement(descriptor, 5, JobDetailsSerializer, value.jobDetails)

        encodeSerializableElement(descriptor, 6, jobHistorySerializer, value.jobStates)

        encodeSerializableElement(descriptor, 7, JobMetadataSerializer, value.metadata)
        value.amountOfRetries?.let {
            encodeIntElement(descriptor, 8, it)
        }
        value.recurringJobId.getOrNull()?.let {
            encodeStringElement(descriptor, 9, it)
        }
    }

    override fun deserialize(decoder: Decoder): Job = decoder.decodeStructure(descriptor) {
        lateinit var id: UUID
        var version = -1
        lateinit var jobName: String
        var labels: List<String>? = null
        lateinit var jobDetails: JobDetails
        lateinit var jobHistory: List<JobState>
        lateinit var metadata: Map<String, Any>
        var amountOfRetries: Int? = null
        var recurringJobId: String? = null

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
                7 -> metadata = decodeSerializableElement(descriptor, 7, JobMetadataSerializer)
                8 -> amountOfRetries = decodeNullableSerializableElement(descriptor, 8, Int.serializer())
                9 -> recurringJobId = decodeNullableSerializableElement(descriptor, 8, String.serializer())
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
            this.jobName = jobName
            this.amountOfRetries = amountOfRetries
            setRecurringJobId(recurringJobId)
            if (labels != null) this.labels = labels
        }
    }
}
