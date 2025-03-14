package org.jobrunr.kotlin.serialization.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlin.reflect.KClass

@OptIn(ExperimentalSerializationApi::class)
open class AnySerializer<T : Any> : KSerializer<Any> {
	override val descriptor =
		buildClassSerialDescriptor(AnySerializer::class.qualifiedName!!) {
			element("@class", String.serializer().descriptor)
			element("value", buildClassSerialDescriptor("Any"))
		}

	@Suppress("UNCHECKED_CAST")
	override fun serialize(encoder: Encoder, value: Any) {
		val composite = encoder.beginStructure(descriptor)
		
		composite.encodeStringElement(descriptor, 0, value.javaClass.name)
		
		fun <T : Any> encode() =
			composite.encodeSerializableElement(
				descriptor,
				1,
				encoder.serializersModule.serializer(value::class as KClass<T>)
					?: error("No serializer found for ${value::class.qualifiedName}"),
				value as T
			)
		encode<Any>()
		
		composite.endStructure(descriptor)
	}

	@Suppress("UNCHECKED_CAST")
	override fun deserialize(decoder: Decoder): Any = decoder.decodeStructure(descriptor) {
		lateinit var type: String

		fun <T : Any> decode(type: String): T {
			val serializer = decoder.serializersModule.serializer(Class.forName(type).kotlin as KClass<T>)
				?: error("No serializer found for $type")
			return decodeSerializableElement(descriptor, 1, serializer)
		}
		
		while (true) {
			when (val index = decodeElementIndex(descriptor)) {
				CompositeDecoder.DECODE_DONE -> break
				0 -> type = decodeStringElement(descriptor, 0)
				1 -> return@decodeStructure decode<T>(type)
				else -> error("Unexpected index $index")
			}
		}
		
		error("Unexpected end of input")
	}
}
