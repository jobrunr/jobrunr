package org.jobrunr.kotlin.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.ClassSerialDescriptorBuilder
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import org.jobrunr.jobs.states.EnqueuedState
import java.time.Instant

fun ClassSerialDescriptorBuilder.abstractJobStateElements() {
	element("state", String.serializer().descriptor)
	element("createdAt", String.serializer().descriptor)
}

object EnqueuedStateSerializer : KSerializer<EnqueuedState> {
	override val descriptor = buildClassSerialDescriptor(EnqueuedState::class.qualifiedName!!) {
		abstractJobStateElements()
	}

	override fun serialize(encoder: Encoder, value: EnqueuedState) = encoder.encodeStructure(descriptor) {
		encodeStringElement(descriptor, 0, value.name.name)
		encodeStringElement(descriptor, 1, value.createdAt.toString())
	}

	override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
		EnqueuedState().apply {
			while (true) {
				when (val index = decodeElementIndex(descriptor)) {
					0 -> decodeStringElement(descriptor, 0)
					1 -> createdAt = Instant.parse(decodeStringElement(descriptor, 1))
					CompositeDecoder.DECODE_DONE -> break
				}
			}
		}
	}
}