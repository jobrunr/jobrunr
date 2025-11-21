package org.jobrunr.kotlin.serialization.jobs

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import org.jobrunr.jobs.JobParameter
import org.jobrunr.jobs.exceptions.JobParameterNotDeserializableException
import org.jobrunr.kotlin.serialization.utils.ClassDiscriminatedContextualSerializer
import org.jobrunr.kotlin.serialization.utils.serializer
import org.jobrunr.utils.mapper.JobParameterJsonMapperException
import org.jobrunr.utils.mapper.JsonMapperUtils
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
fun SerializersModule.jobArgumentValueSerializer(className: String, actualClassName: String?): KSerializer<Any>? {
    return runCatching { serializer(Class.forName(actualClassName).kotlin as KClass<Any>) }.getOrNull()
        ?: try {
            serializer(Class.forName(className).kotlin as KClass<Any>)
        } catch (e: ClassNotFoundException) {
            throw IllegalArgumentException("Class not found: $className", e)
        }
}

object JobParameterSerializer : KSerializer<JobParameter> {
    override val descriptor = buildClassSerialDescriptor(JobParameter::class.qualifiedName!!) {
        element("className", String.serializer().descriptor)
        element("actualClassName", String.serializer().descriptor)
        element("object", ClassDiscriminatedContextualSerializer.descriptor)
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

    override fun deserialize(decoder: Decoder): JobParameter {
        require(decoder is JsonDecoder)

        val jsonElement = decoder.decodeJsonElement()

        if (jsonElement is JsonObject) {
            val jsonObject = jsonElement.jsonObject
            val className = jsonObject["className"]!!.jsonPrimitive.content
            val actualClassName = jsonObject["actualClassName"]?.jsonPrimitive?.content
            val `object`: Any?
            try {
                val serializer = decoder.serializersModule.jobArgumentValueSerializer(className, actualClassName)
                    ?: throw IllegalStateException("No serializer for ${JsonMapperUtils.getActualClassName(className, actualClassName)}")

                val payload = jsonObject["object"]

                `object` = if (payload != null) decoder.json.decodeFromJsonElement(serializer, payload) else null
            } catch (e: Exception) {
                return JobParameter(className, actualClassName, jsonObject["object"], JobParameterNotDeserializableException(className, e))
            }

            return JobParameter(
                className,
                actualClassName,
                `object`
            )
        }

        error("Unexpected json object: $jsonElement")
    }
}