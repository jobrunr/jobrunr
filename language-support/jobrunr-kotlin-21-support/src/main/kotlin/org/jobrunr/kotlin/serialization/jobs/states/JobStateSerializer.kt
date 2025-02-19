@file:OptIn(ExperimentalUuidApi::class, ExperimentalSerializationApi::class)

package org.jobrunr.kotlin.serialization.jobs.states

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.decodeStructure
import org.jobrunr.jobs.states.*
import org.jobrunr.kotlin.serialization.misc.DurationSerializer
import org.jobrunr.kotlin.serialization.misc.InstantSerializer
import org.jobrunr.kotlin.serialization.utils.FieldBasedSerializer
import java.time.Duration
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

abstract class StateSerializer<State : AbstractJobState>(
	kClass: KClass<State>,
	private vararg val fields: Field<State, out Any>,
) : FieldBasedSerializer<State>(
	kClass,
	listOf(
		Field("name", String.serializer()) { it.name.name },
		Field("createdAt", InstantSerializer) { it.createdAt },
		*fields
	)
) {
	protected fun CompositeDecoder.handleDeserialization(callback: (field: Field<State, out Any>, index: Int) -> Unit): Instant {
		lateinit var createdAt: Instant
		while (true) {
			when (val index = decodeElementIndex(descriptor)) {
				CompositeDecoder.DECODE_DONE -> break
				descriptor.getElementIndex("name") -> decodeStringElement(descriptor, index)
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

object DeletedStateSerializer : StateSerializer<DeletedState>(
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
		
		DeletedState(reason).apply {
			this.createdAt = createdAt
		}
	}
}

object EnqueuedStateSerializer : StateSerializer<EnqueuedState>(EnqueuedState::class) {
	override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
		EnqueuedState().apply { 
			createdAt = handleDeserialization { _, _ -> }
		}
	}
}

object FailedStateSerializer : StateSerializer<FailedState>(
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

		FailedState(message, exceptionType, exceptionMessage, exceptionCauseType, exceptionCauseMessage, stackTrace, doNotRetry).apply {
			this.createdAt = createdAt
		}
	}
}

object ProcessingStateSerializer : StateSerializer<ProcessingState>(
	ProcessingState::class,
	Field("serverId", Uuid.serializer()) { it.serverId.toKotlinUuid() },
	Field("serverName", String.serializer()) { it.serverName },
	Field("updatedAt", InstantSerializer) { it.updatedAt },
) {
	override fun deserialize(decoder: Decoder): ProcessingState = decoder.decodeStructure(descriptor) {
		lateinit var serverId: Uuid
		lateinit var serverName: String
		lateinit var updatedAt: Instant
		val createdAt = handleDeserialization { field, index ->
			when (field.name) {
				"serverId" -> serverId = decodeSerializableElement(descriptor, index, Uuid.serializer())
				"serverName" -> serverName = decodeStringElement(descriptor, index)
				"updatedAt" -> updatedAt = decodeSerializableElement(descriptor, index, InstantSerializer)
				else -> error("Unknown field: $field")
			}
		}

		ProcessingState(serverId.toJavaUuid(), serverName).apply {
			this.createdAt = createdAt
			this.updatedAt = updatedAt
		}
	}
}

object ScheduledStateSerializer : StateSerializer<ScheduledState>(
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

		ScheduledState(scheduledAt, reason).apply {
			this.createdAt = createdAt
		}
	}
}

object SucceededStateSerializer : StateSerializer<SucceededState>(
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
		
		SucceededState(latencyDuration, processDuration).apply {
			this.createdAt = createdAt
		}
	}
}
