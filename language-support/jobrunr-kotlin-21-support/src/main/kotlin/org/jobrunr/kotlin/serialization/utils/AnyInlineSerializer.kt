package org.jobrunr.kotlin.serialization.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
class AnyInlineSerializer<T : Any> : KSerializer<T> {
	override val descriptor = buildSerialDescriptor(AnyInlineSerializer::class.qualifiedName!!, SerialKind.CONTEXTUAL) {}

	override fun serialize(encoder: Encoder, value: T) {
		encoder.encodeSerializableValue(
			encoder.serializersModule.serializer(value::class) as? KSerializer<T>
				?: error("No serializer found for ${value::class.qualifiedName}"),
			value
		)
	}

	override fun deserialize(decoder: Decoder) = TODO("Not yet implemented")
}
