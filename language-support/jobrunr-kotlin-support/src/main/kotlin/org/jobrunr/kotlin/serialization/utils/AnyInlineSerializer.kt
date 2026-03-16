package org.jobrunr.kotlin.serialization.utils

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(InternalSerializationApi::class)
class AnyInlineSerializer<T : Any> : KSerializer<T> {
    override val descriptor = buildSerialDescriptor(Any::class.qualifiedName!!, SerialKind.CONTEXTUAL) {}

    @Suppress("UNCHECKED_CAST")
    override fun serialize(encoder: Encoder, value: T) {
        encoder.encodeSerializableValue(
            encoder.serializersModule.serializer(value::class) as? KSerializer<T>
                ?: error("No serializer found for ${value::class.qualifiedName}"),
            value
        )
    }

    override fun deserialize(decoder: Decoder) = throw DeserializationUnsupportedException()
}
