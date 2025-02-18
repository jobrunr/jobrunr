package org.jobrunr.jobs.mappers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.jobrunr.stubs.TestService
import java.nio.file.Path

@Serializable
object IllegalWorkSerializer : KSerializer<TestService.IllegalWork> {
	override val descriptor = buildClassSerialDescriptor("TestService.IllegalWork") {
		element("number", Long.serializer().descriptor)
		element("illegalWork", buildClassSerialDescriptor("TestService.IllegalWork") {})
	}

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

val testModule = SerializersModule {
	contextual(IllegalWorkSerializer)
	contextual(PathSerializer)
}
