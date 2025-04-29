package org.jobrunr.kotlin.serialization.jobs

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.SerializersModule
import org.jobrunr.jobs.JobParameter
import org.jobrunr.jobs.JobParameterNotDeserializableException
import org.jobrunr.kotlin.serialization.utils.AnySerializer
import org.jobrunr.kotlin.serialization.utils.serializer
import org.jobrunr.utils.mapper.JobParameterJsonMapperException
import org.jobrunr.utils.mapper.JsonMapperUtils
import kotlin.reflect.KClass

fun SerializersModule.jobArgumentValueSerializer(className: String, actualClassName: String?): KSerializer<Any>? {
    return runCatching { serializer(Class.forName(actualClassName).kotlin as KClass<Any>) }.getOrNull()
        ?: serializer(Class.forName(className).kotlin as KClass<Any>)
}

object JobParameterSerializer : KSerializer<JobParameter> {
    override val descriptor = buildClassSerialDescriptor(JobParameter::class.qualifiedName!!) {
        element("className", String.serializer().descriptor)
        element("actualClassName", String.serializer().descriptor)
        element("object", AnySerializer<Any>().descriptor)
    }

    override fun serialize(encoder: Encoder, value: JobParameter) = encoder.encodeStructure(descriptor) {
        encodeStringElement(descriptor, 0, value.className)
        value.actualClassName?.let { encodeStringElement(descriptor, 1, it) }
        try {
            value.`object`?.let {
                val serializer = encoder.serializersModule.jobArgumentValueSerializer(value.className, value.actualClassName)
                    ?: throw IllegalStateException("No serializer for ${JsonMapperUtils.getActualClassName(value.className, value.actualClassName)}")
                encodeSerializableElement(descriptor, 2, serializer, it)
            }
        } catch (e: Exception) {
            throw JobParameterJsonMapperException("The job parameters are not serializable.", e)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): JobParameter = decoder.decodeStructure(descriptor) {
        lateinit var className: String
        var actualClassName: String? = null
        var `object`: Any? = null

        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> className = decodeStringElement(descriptor, 0)
                1 -> actualClassName = decodeNullableSerializableElement(descriptor, 1, String.serializer())
                CompositeDecoder.DECODE_DONE, 2 -> {
                    `object` = `object` ?: try {
                        val serializer =
                            decoder.serializersModule.jobArgumentValueSerializer(className, actualClassName)
                                ?: throw IllegalStateException("No serializer for ${JsonMapperUtils.getActualClassName(className, actualClassName)}")
                        decodeNullableSerializableElement(descriptor, 2, serializer)
                    } catch (e: Exception) {
                        return@decodeStructure JobParameter(
                            JobParameterNotDeserializableException(JsonMapperUtils.getActualClassName(className, actualClassName), e.message)
                        )
                    }
                    if (index == CompositeDecoder.DECODE_DONE) break
                }

                else -> error("Unexpected index $index")
            }
        }

        JobParameter(
            className,
            actualClassName,
            `object`
        )
    }
}