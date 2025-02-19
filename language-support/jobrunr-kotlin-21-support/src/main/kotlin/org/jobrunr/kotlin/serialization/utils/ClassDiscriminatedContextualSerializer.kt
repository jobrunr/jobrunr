package org.jobrunr.kotlin.serialization.utils

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure

object ClassDiscriminatedContextualSerializer : KSerializer<Any> {
	@OptIn(InternalSerializationApi::class)
	override val descriptor = buildClassSerialDescriptor(ClassDiscriminatedContextualSerializer::class.qualifiedName!!) {
		element("@class", String.serializer().descriptor)
		element(
			"value",
			buildSerialDescriptor("AnyContextual", SerialKind.CONTEXTUAL)
		)
	}

	val anySerializer = AnySerializer<Any>()

	override fun serialize(encoder: Encoder, value: Any) {
		val serializer = encoder.serializersModule.serializer(value::class)
		@Suppress("UNCHECKED_CAST")
		fun <T : Any> serialize() = ((serializer ?: anySerializer) as KSerializer<T>)
			.serialize(encoder, value as T)
		
		serialize<Any>()
	}

	override fun deserialize(decoder: Decoder): Any = decoder.decodeStructure(descriptor) {
		lateinit var type: String

		while (true) {
			when (val index = decodeElementIndex(descriptor)) {
				CompositeDecoder.DECODE_DONE -> break
				0 -> {
					type = decodeStringElement(descriptor, 0)
					
					val serializer = decoder.serializersModule.serializer(Class.forName(type).kotlin)
						?: anySerializer
					
					if (serializer is PolymorphicContinuationDeserializer) {
						return@decodeStructure with (serializer) {
							continueDecode()
						}
					}
				}
				else -> error("Unexpected index $index")
			}
		}

		error("Unexpected end of input for type $type")
	}

	interface PolymorphicContinuationSerializer<T> {
		fun CompositeEncoder.continueEncode(value: T)
	}
	
	interface PolymorphicContinuationDeserializer {
		fun CompositeDecoder.continueDecode(): Any
	}
}
