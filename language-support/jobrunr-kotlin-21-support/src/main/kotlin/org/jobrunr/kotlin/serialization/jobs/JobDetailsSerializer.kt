package org.jobrunr.kotlin.serialization.jobs

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import org.jobrunr.jobs.JobDetails
import org.jobrunr.jobs.JobParameter

@OptIn(ExperimentalSerializationApi::class)
object JobDetailsSerializer : KSerializer<JobDetails> {
    override val descriptor = buildClassSerialDescriptor(JobDetails::class.qualifiedName!!) {
        element("className", String.serializer().descriptor)
        element("staticFieldName", String.serializer().descriptor)
        element("methodName", String.serializer().descriptor)
        element("jobParameters", ListSerializer(JobParameterSerializer).descriptor)
        element("cacheable", Boolean.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: JobDetails) = encoder.encodeStructure(descriptor) {
        encodeStringElement(descriptor, 0, value.className)
        if (value.staticFieldName != null)
            encodeStringElement(descriptor, 1, value.staticFieldName)
        encodeStringElement(descriptor, 2, value.methodName)
        encodeSerializableElement(descriptor, 3, ListSerializer(JobParameterSerializer), value.jobParameters)
        encodeBooleanElement(descriptor, 4, value.cacheable)
    }

    override fun deserialize(decoder: Decoder): JobDetails = decoder.decodeStructure(descriptor) {
        lateinit var className: String
        var staticFieldName: String? = null
        lateinit var methodName: String
        lateinit var jobParameters: List<JobParameter>
        var cacheable = false

        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> className = decodeStringElement(descriptor, 0)
                1 -> staticFieldName = decodeNullableSerializableElement(descriptor, 1, String.serializer())
                2 -> methodName = decodeStringElement(descriptor, 2)
                3 -> jobParameters = decodeSerializableElement(descriptor, 3, ListSerializer(JobParameterSerializer))
                4 -> cacheable = decodeBooleanElement(descriptor, 4)
                else -> error("Unexpected index $index")
            }
        }

        JobDetails(className, staticFieldName, methodName, jobParameters).apply {
            this.cacheable = cacheable
        }
    }
}
