package org.jobrunr.kotlin.serialization

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.descriptors.listSerialDescriptor
import kotlinx.serialization.encoding.*
import org.jobrunr.jobs.context.JobDashboardLogger
import java.time.Instant

object JobDashboardLogLinesSerializer : KSerializer<JobDashboardLogger.JobDashboardLogLines> {
	@OptIn(ExperimentalSerializationApi::class)
	override val descriptor = listSerialDescriptor(JobDashboardLogLineSerializer.descriptor)

	override fun serialize(encoder: Encoder, value: JobDashboardLogger.JobDashboardLogLines) =
		encoder.encodeCollection(descriptor, collection = value.logLines) { i, line ->
			encodeSerializableElement(descriptor, i, JobDashboardLogLineSerializer, line)
		}

	override fun deserialize(decoder: Decoder): JobDashboardLogger.JobDashboardLogLines {
		TODO("Not yet implemented")
	}
}

object JobDashboardLogLineSerializer : KSerializer<JobDashboardLogger.JobDashboardLogLine> {
	override val descriptor = buildClassSerialDescriptor(JobDashboardLogger.JobDashboardLogLine::class.qualifiedName!!) {
		element("level", LevelSerializer.descriptor)
		element("logInstance", String.serializer().descriptor)
		element("logMessage", String.serializer().descriptor)
	}

	override fun serialize(encoder: Encoder, value: JobDashboardLogger.JobDashboardLogLine) = encoder.encodeStructure(descriptor) {
		encodeSerializableElement(descriptor, 0, LevelSerializer, value.level)
		encodeStringElement(descriptor, 1, value.logInstant.toString())
		encodeStringElement(descriptor, 2, value.logMessage)
	}

	override fun deserialize(decoder: Decoder): JobDashboardLogger.JobDashboardLogLine = decoder.decodeStructure(descriptor) {
		lateinit var level: JobDashboardLogger.Level
		lateinit var logInstance: String
		lateinit var logMessage: String

		while (true) {
			when (val index = decodeElementIndex(descriptor)) {
				0 -> level = decodeSerializableElement(descriptor, 0, LevelSerializer)
				1 -> logInstance = decodeStringElement(descriptor, 1)
				2 -> logMessage = decodeStringElement(descriptor, 2)
				CompositeDecoder.DECODE_DONE -> break
				else -> error("Unexpected index: $index")
			}
		}

		JobDashboardLogger.JobDashboardLogLine(level, Instant.parse(logInstance), logMessage)
	}
}

object LevelSerializer : KSerializer<JobDashboardLogger.Level> {
	@OptIn(InternalSerializationApi::class)
	override val descriptor = buildSerialDescriptor(JobDashboardLogger.Level::class.qualifiedName!!, SerialKind.ENUM)

	override fun serialize(encoder: Encoder, value: JobDashboardLogger.Level) = encoder.encodeString(value.name)

	override fun deserialize(decoder: Decoder): JobDashboardLogger.Level = JobDashboardLogger.Level.valueOf(decoder.decodeString())
}