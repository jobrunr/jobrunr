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
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import org.jobrunr.jobs.JobDetails
import org.jobrunr.jobs.RecurringJob
import org.jobrunr.kotlin.serialization.misc.InstantSerializer
import org.jobrunr.kotlin.serialization.utils.ClassDiscriminatedContextualSerializer
import java.time.Instant

@OptIn(ExperimentalSerializationApi::class)
object RecurringJobSerializer : KSerializer<RecurringJob>, ClassDiscriminatedContextualSerializer.PolymorphicContinuationSerializer<RecurringJob> {
    @OptIn(InternalSerializationApi::class)
    val createdByDescriptor = buildSerialDescriptor(RecurringJob.CreatedBy::class.qualifiedName!!, SerialKind.ENUM)

    val buildClassSerialDescriptorElements = fun ClassSerialDescriptorBuilder.() {
        element("id", String.serializer().descriptor)
        element("version", Int.serializer().descriptor)
        element("jobName", String.serializer().descriptor)
        element("amountOfRetries", Int.serializer().descriptor)
        element("jobSignature", String.serializer().descriptor)
        element("labels", SetSerializer(String.serializer()).descriptor)
        element("jobDetails", JobDetailsSerializer.descriptor)
        element("scheduleExpression", String.serializer().descriptor)
        element("zoneId", String.serializer().descriptor)
        element("createdBy", createdByDescriptor)
        element("createdAt", InstantSerializer.descriptor)
    }

    override val descriptor = buildClassSerialDescriptor(RecurringJob::class.qualifiedName!!, builderAction = buildClassSerialDescriptorElements)

    override fun serialize(encoder: Encoder, value: RecurringJob) = encoder.encodeStructure(descriptor) {
        continueEncode(value)
    }

    override fun CompositeEncoder.continueEncode(value: RecurringJob) {
        encodeStringElement(descriptor, 0, value.id)
        encodeIntElement(descriptor, 1, value.version)
        encodeStringElement(descriptor, 2, value.jobName)
        value.amountOfRetries?.let {
            encodeIntElement(descriptor, 3, it)
        }
        encodeStringElement(descriptor, 4, value.jobSignature)
        encodeSerializableElement(descriptor, 5, ListSerializer(String.serializer()), value.labels)
        encodeSerializableElement(descriptor, 6, JobDetailsSerializer, value.jobDetails)
        encodeStringElement(descriptor, 7, value.scheduleExpression)
        encodeStringElement(descriptor, 8, value.zoneId)
        encodeStringElement(descriptor, 9, value.createdBy.name)
        encodeSerializableElement(descriptor, 10, InstantSerializer, value.createdAt)
    }

    override fun deserialize(decoder: Decoder): RecurringJob = decoder.decodeStructure(descriptor) {
        lateinit var id: String
        var version = 0
        lateinit var jobName: String
        var amountOfRetries: Int? = null
        var jobSignature: String? = null
        var labels: List<String>? = null
        lateinit var jobDetails: JobDetails
        lateinit var scheduleExpression: String
        lateinit var zoneId: String
        var createdBy: RecurringJob.CreatedBy = RecurringJob.CreatedBy.API
        var createdAt: Instant = Instant.now()

        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> id = decodeStringElement(descriptor, 0)
                1 -> version = decodeIntElement(descriptor, 1)
                2 -> jobName = decodeStringElement(descriptor, 2)
                3 -> amountOfRetries = decodeNullableSerializableElement(descriptor, 3, Int.serializer())
                4 -> jobSignature = decodeStringElement(descriptor, 4)
                5 -> labels = decodeSerializableElement(descriptor, 5, ListSerializer(String.serializer()))
                6 -> jobDetails = decodeSerializableElement(descriptor, 6, JobDetailsSerializer)
                7 -> scheduleExpression = decodeStringElement(descriptor, 7)
                8 -> zoneId = decodeStringElement(descriptor, 8)
                9 -> createdBy = decodeNullableSerializableElement(descriptor, 9, String.serializer())?.let(RecurringJob.CreatedBy::valueOf) ?: createdBy
                10 -> createdAt = decodeSerializableElement(descriptor, 10, InstantSerializer)
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
