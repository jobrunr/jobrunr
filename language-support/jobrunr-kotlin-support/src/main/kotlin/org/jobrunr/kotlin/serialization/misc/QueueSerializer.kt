package org.jobrunr.kotlin.serialization.misc

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class QueueSerializer<E>(private val elementSerializer: KSerializer<E>) : KSerializer<Queue<E>> {
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = listSerialDescriptor(elementSerializer.descriptor)

    override fun deserialize(decoder: Decoder): Queue<E> = decoder.decodeStructure(descriptor) {
        val list = LinkedList<E>()
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                else -> list.add(decodeSerializableElement(descriptor, index, elementSerializer))
            }
        }
        list
    }

    override fun serialize(encoder: Encoder, value: Queue<E>) = encoder.encodeStructure(descriptor) {
        value.forEachIndexed { index, element ->
            encodeSerializableElement(descriptor, index, elementSerializer, element)
        }
    }
}

class ConcurrentLinkedQueueSerializer<E>(private val elementSerializer: KSerializer<E>) : KSerializer<ConcurrentLinkedQueue<E>> {
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = listSerialDescriptor(elementSerializer.descriptor)

    override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
        val list = ConcurrentLinkedQueue<E>()
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                else -> list.add(decodeSerializableElement(descriptor, index, elementSerializer))
            }
        }
        list
    }

    override fun serialize(encoder: Encoder, value: ConcurrentLinkedQueue<E>) = encoder.encodeStructure(descriptor) {
        value.forEachIndexed { index, element ->
            encodeSerializableElement(descriptor, index, elementSerializer, element)
        }
    }
}
