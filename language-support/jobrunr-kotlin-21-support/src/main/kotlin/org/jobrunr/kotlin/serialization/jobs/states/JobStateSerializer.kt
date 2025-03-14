@file:OptIn(ExperimentalSerializationApi::class)

package org.jobrunr.kotlin.serialization.jobs.states

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.decodeStructure
import org.jobrunr.jobs.states.*
import org.jobrunr.kotlin.serialization.misc.DurationSerializer
import org.jobrunr.kotlin.serialization.misc.InstantSerializer
import org.jobrunr.kotlin.serialization.misc.UUIDSerializer
import org.jobrunr.kotlin.serialization.utils.FieldBasedSerializer
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.reflect.KClass

abstract class JobStateSerializer<State : AbstractJobState>(
	kClass: KClass<State>,
	private vararg val fields: Field<State, out Any>,
) : FieldBasedSerializer<State>(
	kClass,
	listOf(
		Field("state", String.serializer()) { it.name.name },
		Field("createdAt", InstantSerializer) { it.createdAt },
		*fields
	)
) {
	protected fun CompositeDecoder.handleDeserialization(callback: (field: Field<State, out Any>, index: Int) -> Unit): Instant {
		lateinit var createdAt: Instant
		while (true) {
			when (val index = decodeElementIndex(descriptor)) {
				CompositeDecoder.DECODE_DONE -> break
				descriptor.getElementIndex("state") -> decodeStringElement(descriptor, index)
				descriptor.getElementIndex("createdAt") -> createdAt = decodeSerializableElement(descriptor, 1, InstantSerializer)
				else -> {
					val elementName = descriptor.getElementName(index)
					val field = fields.singleOrNull { 
						it.name == elementName
					} ?: error("Unknown field: $elementName")
					callback(field, index)
				}
			}
		}
		return createdAt
	}
}

object DeletedStateSerializer : JobStateSerializer<DeletedState>(
	DeletedState::class,
	Field("reason", String.serializer()) { it.reason },
) {
	override fun deserialize(decoder: Decoder): DeletedState = decoder.decodeStructure(descriptor) {
		lateinit var reason: String
		val createdAt = handleDeserialization { field, index ->
			when (field.name) {
				"reason" -> reason = decodeStringElement(descriptor, index)
				else -> error("Unknown field: ${field.name}")
			}
		}

		DeletedState(createdAt, reason)
	}
}

object EnqueuedStateSerializer : JobStateSerializer<EnqueuedState>(EnqueuedState::class) {
	override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
		val createdAt = handleDeserialization { _, _ -> }
		EnqueuedState(createdAt)
	}
}

object FailedStateSerializer : JobStateSerializer<FailedState>(
	FailedState::class,
	Field("message", String.serializer()) { it.message },
	Field("exceptionType", String.serializer()) { it.exceptionType },
	Field("exceptionMessage", String.serializer()) { it.exceptionMessage },
	Field("exceptionCauseType", String.serializer(), nullable = true) { it.exceptionCauseType },
	Field("exceptionCauseMessage", String.serializer(), nullable = true) { it.exceptionCauseMessage },
	Field("stackTrace", String.serializer()) { it.stackTrace },
	Field("doNotRetry", Boolean.serializer()) { it.mustNotRetry() },
) {
	override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
		lateinit var message: String
		lateinit var exceptionType: String
		lateinit var exceptionMessage: String
		var exceptionCauseType: String? = null
		var exceptionCauseMessage: String? = null
		lateinit var stackTrace: String
		var doNotRetry = false
		val createdAt = handleDeserialization { field, index ->
			when (field.name) {
				"message" -> message = decodeStringElement(descriptor, index)
				"exceptionType" -> exceptionType = decodeStringElement(descriptor, index)
				"exceptionMessage" -> exceptionMessage = decodeStringElement(descriptor, index)
				"exceptionCauseType" -> exceptionCauseType = decodeNullableSerializableElement(descriptor, index, String.serializer())
				"exceptionCauseMessage" -> exceptionCauseMessage = decodeNullableSerializableElement(descriptor, index, String.serializer())
				"stackTrace" -> stackTrace = decodeStringElement(descriptor, index)
				"doNotRetry" -> doNotRetry = decodeBooleanElement(descriptor, index)
				else -> error("Unknown field: ${field.name}")
			}
		}

		FailedState(createdAt, message, exceptionType, exceptionMessage, exceptionCauseType, exceptionCauseMessage, stackTrace, doNotRetry)
	}
}

object ProcessingStateSerializer : JobStateSerializer<ProcessingState>(
	ProcessingState::class,
	Field("serverId", UUIDSerializer) { it.serverId },
	Field("serverName", String.serializer()) { it.serverName },
	Field("updatedAt", InstantSerializer) { it.updatedAt },
) {
	override fun deserialize(decoder: Decoder): ProcessingState = decoder.decodeStructure(descriptor) {
		lateinit var serverId: UUID
		lateinit var serverName: String
		lateinit var updatedAt: Instant
		val createdAt = handleDeserialization { field, index ->
			when (field.name) {
				"serverId" -> serverId = decodeSerializableElement(descriptor, index, UUIDSerializer)
				"serverName" -> serverName = decodeStringElement(descriptor, index)
				"updatedAt" -> updatedAt = decodeSerializableElement(descriptor, index, InstantSerializer)
				else -> error("Unknown field: $field")
			}
		}

		ProcessingState(createdAt, updatedAt, serverId, serverName)
	}
}

object ScheduledStateSerializer : JobStateSerializer<ScheduledState>(
	ScheduledState::class,
	Field("scheduledAt", InstantSerializer) { it.scheduledAt },
	Field("reason", String.serializer(), nullable = true) { it.reason },
) {
	override fun deserialize(decoder: Decoder): ScheduledState = decoder.decodeStructure(descriptor) {
		lateinit var scheduledAt: Instant
		var reason: String? = null
		val createdAt = handleDeserialization { field, index ->
			when (field.name) {
				"scheduledAt" -> scheduledAt = decodeSerializableElement(descriptor, index, InstantSerializer)
				"reason" -> reason = decodeNullableSerializableElement(descriptor, index, String.serializer())
				else -> error("Unknown field: ${field.name}")
			}
		}

		ScheduledState(createdAt, scheduledAt, reason, null)
	}
}

object SucceededStateSerializer : JobStateSerializer<SucceededState>(
	SucceededState::class,
	Field("latencyDuration", DurationSerializer) { it.latencyDuration },
	Field("processDuration", DurationSerializer) { it.processDuration},
) {
	override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
		lateinit var latencyDuration: Duration
		lateinit var processDuration: Duration
		val createdAt = handleDeserialization { field, index ->
			when (field.name) {
				"latencyDuration" -> latencyDuration = decodeSerializableElement(descriptor, index, DurationSerializer)
				"processDuration" -> processDuration = decodeSerializableElement(descriptor, index, DurationSerializer)
				else -> error("Unknown field: ${field.name}")
			}
		}

		SucceededState(createdAt, latencyDuration, processDuration)
	}
}
