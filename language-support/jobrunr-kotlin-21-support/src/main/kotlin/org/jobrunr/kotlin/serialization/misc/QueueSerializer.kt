package org.jobrunr.kotlin.serialization.misc

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.*
import java.util.*

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
class QueueSerializer<E>(private val elementSerializer: KSerializer<E>) : KSerializer<Queue<E>> {
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
