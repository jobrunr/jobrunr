package org.jobrunr.kotlin.utils.mapper

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializerOrNull
import kotlin.reflect.KClass

@OptIn(ExperimentalSerializationApi::class)
class AnySerializer<T : Any> : KSerializer<Any> {
	private val fallbackSerializer: KSerializer<T>? = null
	private val typeArgumentsSerializers: List<KSerializer<*>> = emptyList()

	override val descriptor =
		buildClassSerialDescriptor("org.jobrunr.kotlin.utils.mapper.AnySerializer") {
			element("_t", String.serializer().descriptor)
			element("value", buildClassSerialDescriptor("Any"))
		}

	@OptIn(InternalSerializationApi::class)
	@Suppress("UNCHECKED_CAST")
	private fun <T : Any> serializer(kClass: KClass<T>, serializersModule: SerializersModule): KSerializer<T> =
		kClass.serializerOrNull()
			?: serializersModule.getPolymorphic(Any::class, kClass.qualifiedName) as KSerializer<T>?
			?: serializersModule.getContextual(kClass, typeArgumentsSerializers)
			?: fallbackSerializer as KSerializer<T>?
			?: error("No serializer found for ${kClass.qualifiedName}")

	@Suppress("UNCHECKED_CAST")
	override fun serialize(encoder: Encoder, value: Any) {
		val composite = encoder.beginStructure(descriptor)
		
		composite.encodeStringElement(descriptor, 0, value.javaClass.name)
		
		fun <T : Any> encode() =
			composite.encodeSerializableElement(
				descriptor,
				1,
				serializer(value::class as KClass<T>, encoder.serializersModule),
				value as T
			)
		encode<Any>()
		
		composite.endStructure(descriptor)
	}

	@Suppress("UNCHECKED_CAST")
	override fun deserialize(decoder: Decoder): Any = decoder.decodeStructure(descriptor) {
		lateinit var type: String

		fun <T : Any> decode(type: String): T {
			val serializer = serializer(Class.forName(type).kotlin as KClass<T>, decoder.serializersModule)
			return decodeSerializableElement(descriptor, 1, serializer)
		}
		
		while (true) {
			when (val index = decodeElementIndex(descriptor)) {
				// type
				0 -> type = decodeStringElement(descriptor, 0)
				1 -> return@decodeStructure decode<T>(type)
				CompositeDecoder.DECODE_DONE -> break
			}
		}
		
		error("Unexpected end of input")
	}
}

