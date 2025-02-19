package org.jobrunr.kotlin.serialization.jobs.context

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
import org.jobrunr.kotlin.serialization.utils.ClassDiscriminatedContextualSerializer
import org.jobrunr.kotlin.serialization.utils.InstantSerializer
import org.jobrunr.kotlin.serialization.utils.QueueSerializer
import java.time.Instant
import java.util.*

object JobDashboardLogLinesSerializer : KSerializer<JobDashboardLogger.JobDashboardLogLines>, ClassDiscriminatedContextualSerializer.PolymorphicContinuationDeserializer {
	@OptIn(ExperimentalSerializationApi::class)
	override val descriptor = buildClassSerialDescriptor(JobDashboardLogger.JobDashboardLogLines::class.qualifiedName!!) {
		element("@class", String.serializer().descriptor)
		element("logLines", listSerialDescriptor(JobDashboardLogLineSerializer.descriptor))
	}

	override fun serialize(encoder: Encoder, value: JobDashboardLogger.JobDashboardLogLines) = encoder.encodeStructure(descriptor) {
		encodeStringElement(descriptor, 0, value::class.java.typeName)
		encodeSerializableElement(descriptor, 1, QueueSerializer(JobDashboardLogLineSerializer), value.logLines)
	}

	override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
		continueDecode()
	}

	override fun CompositeDecoder.continueDecode(): JobDashboardLogger.JobDashboardLogLines {
		lateinit var logLines: Queue<JobDashboardLogger.JobDashboardLogLine>

		while (true) {
			when (val index = decodeElementIndex(descriptor)) {
				CompositeDecoder.DECODE_DONE -> break
				0 -> decodeStringElement(descriptor, 0)
				1 -> logLines = decodeSerializableElement(descriptor, 1, QueueSerializer(JobDashboardLogLineSerializer))
				else -> error("Unexpected index $index")
			}
		}

		return JobDashboardLogger.JobDashboardLogLines().apply {
			this.logLines.addAll(logLines)
		}
	}
}

object JobDashboardLogLineSerializer : KSerializer<JobDashboardLogger.JobDashboardLogLine> {
	override val descriptor = buildClassSerialDescriptor(JobDashboardLogger.JobDashboardLogLine::class.qualifiedName!!) {
		element("level", LevelSerializer.descriptor)
		element("logInstant", InstantSerializer.descriptor)
		element("logMessage", String.serializer().descriptor)
	}

	override fun serialize(encoder: Encoder, value: JobDashboardLogger.JobDashboardLogLine) = encoder.encodeStructure(descriptor) {
		encodeSerializableElement(descriptor, 0, LevelSerializer, value.level)
		encodeSerializableElement(descriptor, 1, InstantSerializer, value.logInstant)
		encodeStringElement(descriptor, 2, value.logMessage)
	}

	override fun deserialize(decoder: Decoder): JobDashboardLogger.JobDashboardLogLine = decoder.decodeStructure(descriptor) {
		lateinit var level: JobDashboardLogger.Level
		lateinit var logInstant: Instant
		lateinit var logMessage: String

		while (true) {
			when (val index = decodeElementIndex(descriptor)) {
				CompositeDecoder.DECODE_DONE -> break
				0 -> level = decodeSerializableElement(descriptor, 0, LevelSerializer)
				1 -> logInstant = decodeSerializableElement(descriptor, 1, InstantSerializer)
				2 -> logMessage = decodeStringElement(descriptor, 2)
				else -> error("Unexpected index: $index")
			}
		}

		JobDashboardLogger.JobDashboardLogLine(level, logInstant, logMessage)
	}
}

object LevelSerializer : KSerializer<JobDashboardLogger.Level> {
	@OptIn(InternalSerializationApi::class)
	override val descriptor = buildSerialDescriptor(JobDashboardLogger.Level::class.qualifiedName!!, SerialKind.ENUM)

	override fun serialize(encoder: Encoder, value: JobDashboardLogger.Level) = encoder.encodeString(value.name)

	override fun deserialize(decoder: Decoder): JobDashboardLogger.Level = JobDashboardLogger.Level.valueOf(decoder.decodeString())
}
