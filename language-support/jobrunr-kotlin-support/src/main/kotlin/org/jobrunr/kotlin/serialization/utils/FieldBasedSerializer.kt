package org.jobrunr.kotlin.serialization.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlin.reflect.KClass

abstract class FieldBasedSerializer<T : Any>(
    kClass: KClass<T>,
    private val fields: List<Field<T, out Any>>,
) : KSerializer<T>, ClassDiscriminatedContextualSerializer.PolymorphicContinuationSerializer<T> {
    constructor(kClass: KClass<T>, vararg fields: Field<T, out Any>) : this(kClass, fields.toList())

    override val descriptor = buildClassSerialDescriptor(kClass.qualifiedName!!) {
        fields.forEach {
            element(it.name, it.serializer.descriptor)
        }
    }

    override fun serialize(encoder: Encoder, value: T) = encoder.encodeStructure(descriptor) {
        continueEncode(value)
    }

    override fun CompositeEncoder.continueEncode(value: T) {
        fields.forEachIndexed { index, field ->
            @Suppress("UNCHECKED_CAST")
            fun <O> serialize() {
                val serializer = field.serializer as KSerializer<O>
                val fieldValue = field.getter(value) as O?
                if (field.nullable) fieldValue?.let { encodeSerializableElement(descriptor, index, serializer, it) }
                else encodeSerializableElement(descriptor, index, serializer, fieldValue ?: error("Field ${field.name} is null"))
            }

            serialize<Any>()
        }
    }

    override fun deserialize(decoder: Decoder): T = throw DeserializationUnsupportedException()

    data class Field<I, O>(
        val name: String,
        val serializer: KSerializer<O>,
        var nullable: Boolean = false,
        val getter: (I) -> O?
    )
}
