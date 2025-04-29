package org.jobrunr.kotlin.serialization.jobs

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.descriptors.mapSerialDescriptor
import kotlinx.serialization.encoding.*
import org.jobrunr.kotlin.serialization.utils.ClassDiscriminatedContextualSerializer

object JobMetadataSerializer : KSerializer<Map<String, Any>> {
    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override val descriptor = mapSerialDescriptor(
        String.serializer().descriptor,
        buildSerialDescriptor("Any", SerialKind.CONTEXTUAL)
    )

    override fun serialize(encoder: Encoder, value: Map<String, Any>) = encoder.encodeCollection(descriptor, value.size + 1) {
        var index = 0
        encodeStringElement(descriptor, index++, "@class")
        encodeStringElement(descriptor, index++, value::class.qualifiedName!!)
        value.forEach { (k, v) ->
            encodeSerializableElement(descriptor, index++, String.serializer(), k)
            encodeSerializableElement(descriptor, index++, ClassDiscriminatedContextualSerializer, v)
        }
    }

    override fun deserialize(decoder: Decoder): Map<String, Any> = decoder.decodeStructure(descriptor) {
        val map = mutableMapOf<String, Any>()

        var currentKey = ""
        while (true) {
            val index = decodeElementIndex(descriptor)
            when {
                index == CompositeDecoder.DECODE_DONE -> break
                index == 0 || index == 1 -> decodeStringElement(descriptor, 0)
                index % 2 == 0 -> currentKey = decodeStringElement(descriptor, index)
                index % 2 == 1 -> map[currentKey] = decodeSerializableElement(descriptor, index, ClassDiscriminatedContextualSerializer)
            }
        }

        map
    }
}
