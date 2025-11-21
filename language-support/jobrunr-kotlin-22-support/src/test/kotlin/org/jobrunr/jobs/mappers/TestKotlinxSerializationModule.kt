package org.jobrunr.jobs.mappers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.jobrunr.jobs.mappers.JobMapperTest.TestMetadata
import org.jobrunr.kotlin.serialization.misc.InstantSerializer
import org.jobrunr.kotlin.serialization.misc.UUIDSerializer
import org.jobrunr.kotlin.utils.mapper.FileSerializer
import org.jobrunr.stubs.TestService
import java.io.File
import java.nio.file.Path
import java.time.Instant
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

object TestServiceSimpleCommandSerializer : KSerializer<TestService.SimpleCommand> {
    override val descriptor = buildClassSerialDescriptor(TestService.Work::class.qualifiedName!!) {
        element("string", String.serializer().descriptor)
        element("integer", Int.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: TestService.SimpleCommand) = encoder.encodeStructure(descriptor) {
        encodeStringElement(descriptor, 0, value.string)
        encodeIntElement(descriptor, 1, value.integer)
    }

    override fun deserialize(decoder: Decoder): TestService.SimpleCommand = decoder.decodeStructure(descriptor) {
        lateinit var string: String
        var integer = 0

        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> string = decodeStringElement(descriptor, 0)
                1 -> integer = decodeIntElement(descriptor, 1)
                CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index: $index")
            }
        }

        TestService.SimpleCommand(string, integer)
    }
}

object TestMetadataSerializer : KSerializer<TestMetadata> {
    override val descriptor = buildClassSerialDescriptor(TestMetadata::class.qualifiedName!!) {
        element("input", String.serializer().descriptor)
        element("instant", InstantSerializer.descriptor)
        element("path", PathSerializer.descriptor)
        element("file", FileSerializer.descriptor)
    }

    override fun serialize(encoder: Encoder, value: TestMetadata) = encoder.encodeStructure(descriptor) {
        encodeStringElement(descriptor, 0, value.input)
        encodeSerializableElement(descriptor, 1, InstantSerializer, value.instant)
        encodeSerializableElement(descriptor, 2, PathSerializer, value.path)
        encodeSerializableElement(descriptor, 3, FileSerializer, value.file)
    }

    override fun deserialize(decoder: Decoder): TestMetadata = decoder.decodeStructure(descriptor) {
        lateinit var input: String
        lateinit var instant: Instant
        lateinit var path: Path
        lateinit var file: File

        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> input = decodeStringElement(descriptor, 0)
                1 -> instant = decodeSerializableElement(descriptor, 1, InstantSerializer)
                2 -> path = decodeSerializableElement(descriptor, 2, PathSerializer)
                3 -> file = decodeSerializableElement(descriptor, 3, FileSerializer)
                CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index: $index")
            }
        }

        TestMetadata(input, instant, path, file)
    }
}

val testModule = SerializersModule {
    contextual(IllegalWorkSerializer)
    contextual(PathSerializer)
    contextual(TestService.Work::class, TestServiceWorkSerializer)
    contextual(TestService.SimpleCommand::class, TestServiceSimpleCommandSerializer)
    contextual(TestMetadataSerializer)
}
