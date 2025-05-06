package org.jobrunr.kotlin.serialization.jobs

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
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

object JobParameterSerializer : KSerializer<JobParameter> {
	override val descriptor = buildClassSerialDescriptor(JobParameter::class.qualifiedName!!) {
		element("className", String.serializer().descriptor)
		element("actualClassName", String.serializer().descriptor)
		element("object", AnySerializer<Any>().descriptor)
	}

	private fun SerializersModule.serializer(className: String, actualClassName: String): KSerializer<Any>? {
		val actual = JsonMapperUtils.getActualClassName(className, actualClassName)
		val kClass = Class.forName(actual).kotlin
		return serializer(kClass as KClass<Any>)
	}

	private fun Exception.notDeserializable(className: String, actualClassName: String) = JobParameterNotDeserializableException(
		JsonMapperUtils.getActualClassName(className, actualClassName),
		message
	)

	override fun serialize(encoder: Encoder, value: JobParameter) = encoder.encodeStructure(descriptor) {
		encodeStringElement(descriptor, 0, value.className)
		encodeStringElement(descriptor, 1, value.actualClassName)
		try {
			val serializer = encoder.serializersModule.jobParameterSerializer(value.className, value.actualClassName)
				?: throw JobParameterJsonMapperException("No serializer for ${JsonMapperUtils.getActualClassName(value.className, value.actualClassName)}")
			encodeSerializableElement(descriptor, 2, serializer, value.`object`)
		} catch (e: ClassNotFoundException) {
			encodeSerializableElement(
				descriptor,
				2,
				JobParameterNotDeserializableExceptionSerializer,
				e.notDeserializable(value.className, value.actualClassName)
			)
		}
	}

	override fun deserialize(decoder: Decoder): JobParameter = decoder.decodeStructure(descriptor) {
		lateinit var className: String
		lateinit var actualClassName: String
		lateinit var `object`: Any

		while (true) {
			when (val index = decodeElementIndex(descriptor)) {
				CompositeDecoder.DECODE_DONE -> break
				0 -> className = decodeStringElement(descriptor, 0)
				1 -> actualClassName = decodeStringElement(descriptor, 1)
				2 -> `object` = try {
					val elementSerializer = decoder.serializersModule.jobParameterSerializer(className, actualClassName)!!
					decodeSerializableElement(descriptor, 2, elementSerializer)
				} catch (e: Exception) {
					when (e) {
						is ClassNotFoundException, is SerializationException -> {
							val ex = decodeSerializableElement(descriptor, 2, JobParameterNotDeserializableExceptionSerializer)
							return@decodeStructure JobParameter(className, actualClassName, ex)
						}
						else -> throw e
					}
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

fun SerializersModule.jobParameterSerializer(className: String, actualClassName: String): KSerializer<Any>? {
	val actualClass = Class.forName(actualClassName).kotlin as KClass<Any>
	
	return runCatching { serializer(actualClass) }.getOrNull()
		?: serializer(Class.forName(className).kotlin as KClass<Any>)
}
