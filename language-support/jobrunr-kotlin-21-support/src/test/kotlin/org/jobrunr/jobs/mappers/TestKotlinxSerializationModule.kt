package org.jobrunr.jobs.mappers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.jobrunr.kotlin.serialization.misc.UUIDSerializer
import org.jobrunr.stubs.TestService
import java.nio.file.Path
import java.util.*

@Serializable
object IllegalWorkSerializer : KSerializer<TestService.IllegalWork> {
	override val descriptor = buildClassSerialDescriptor("TestService.IllegalWork") {
		element("number", Long.serializer().descriptor)
		element("illegalWork", buildClassSerialDescriptor("TestService.IllegalWork") {})
	}

	@OptIn(ExperimentalSerializationApi::class)
	override fun serialize(encoder: Encoder, value: TestService.IllegalWork) {
		// subclass of SerializationException
		throw MissingFieldException("illegalWork", "illegalWork")
	}

	override fun deserialize(decoder: Decoder): TestService.IllegalWork {
		TODO("Not yet implemented")
	}
}

/**
 * Noop path serializer, would have to be provided by the user
 */
object PathSerializer : KSerializer<Path> {
	override val descriptor = PrimitiveSerialDescriptor("Path", PrimitiveKind.STRING)

	override fun serialize(encoder: Encoder, value: Path) {
		encoder.encodeString(value.toAbsolutePath().toString())
	}

	override fun deserialize(decoder: Decoder): Path = Path.of(decoder.decodeString())
}

object TestServiceWorkSerializer : KSerializer<TestService.Work> {
	override val descriptor = buildClassSerialDescriptor(TestService.Work::class.qualifiedName!!) {
		element("workCount", Int.serializer().descriptor)
		element("someString", String.serializer().descriptor)
		element("uuid", UUIDSerializer.descriptor)
	}

	override fun serialize(encoder: Encoder, value: TestService.Work) = encoder.encodeStructure(descriptor) {
		encodeIntElement(descriptor, 0, value.workCount)
		encodeStringElement(descriptor, 1, value.someString)
		encodeSerializableElement(descriptor, 2, UUIDSerializer, value.uuid)
	}

	override fun deserialize(decoder: Decoder): TestService.Work = decoder.decodeStructure(descriptor) {
		var workCount: Int = -1
		lateinit var someString: String
		lateinit var uuid: UUID

		while (true) {
			when (val index = decodeElementIndex(descriptor)) {
				0 -> workCount = decodeIntElement(descriptor, 0)
				1 -> someString = decodeStringElement(descriptor, 1)
				2 -> uuid = decodeSerializableElement(descriptor, 2, UUIDSerializer)
				CompositeDecoder.DECODE_DONE -> break
				else -> error("Unexpected index: $index")
			}
		}

		TestService.Work(workCount, someString, uuid)
	}
}

val testModule = SerializersModule {
	contextual(IllegalWorkSerializer)
	contextual(PathSerializer)
	contextual(TestService.Work::class, TestServiceWorkSerializer)
}
