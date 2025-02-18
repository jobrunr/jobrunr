package org.jobrunr.kotlin.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import org.jobrunr.jobs.JobParameterNotDeserializableException

object JobParameterNotDeserializableExceptionSerializer : KSerializer<JobParameterNotDeserializableException> {
	override val descriptor = buildClassSerialDescriptor(JobParameterNotDeserializableException::class.qualifiedName!!) {
		element("className", String.serializer().descriptor)
		element("exceptionMessage", String.serializer().descriptor)
	}

	override fun serialize(encoder: Encoder, value: JobParameterNotDeserializableException) = encoder.encodeStructure(descriptor) {
		encodeStringElement(descriptor, 0, value.className)
		encodeStringElement(descriptor, 1, value.exceptionMessage)
	}

	override fun deserialize(decoder: Decoder): JobParameterNotDeserializableException = decoder.decodeStructure(descriptor) {
		lateinit var className: String
		lateinit var exceptionMessage: String
		
		while (true) {
			when (val index = decodeElementIndex(descriptor)) {
				CompositeDecoder.DECODE_DONE -> break
				0 -> className = decodeStringElement(descriptor, 0)
				1 -> exceptionMessage = decodeStringElement(descriptor, 1)
				else -> error("Unexpected index $index")
			}
		}
		
		return@decodeStructure JobParameterNotDeserializableException(className, exceptionMessage)
	}
}