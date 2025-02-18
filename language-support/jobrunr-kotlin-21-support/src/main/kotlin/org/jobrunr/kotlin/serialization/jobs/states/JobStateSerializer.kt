@file:OptIn(ExperimentalUuidApi::class, ExperimentalSerializationApi::class)

package org.jobrunr.kotlin.serialization.jobs.states

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.ClassSerialDescriptorBuilder
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import org.jobrunr.jobs.states.*
import org.jobrunr.kotlin.serialization.utils.DurationSerializer
import java.time.Duration
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

fun ClassSerialDescriptorBuilder.abstractJobStateElements() {
	element("state", String.serializer().descriptor)
	element("createdAt", String.serializer().descriptor)
}

abstract class StateSerializer<State : AbstractJobState>(
	kClass: KClass<State>,
	private vararg val fields: Field,
) : KSerializer<State> {
	override val descriptor = buildClassSerialDescriptor(kClass.qualifiedName!!) {
		abstractJobStateElements()
		fields.forEach {
			element(it.name, it.descriptor)
		}
	}

	override fun serialize(encoder: Encoder, value: State) = encoder.encodeStructure(descriptor) {
		encodeStringElement(descriptor, 0, value.name.name)
		encodeStringElement(descriptor, 1, value.createdAt.toString())
		fields.forEachIndexed { index, field ->
			serializeAdditional(value, field.name, index + 2)
		}
	}

	abstract fun CompositeEncoder.serializeAdditional(state: State, name: String, index: Int)

	protected fun CompositeDecoder.handleDeserialization(callback: (field: Field, index: Int) -> Unit): Instant {
		lateinit var createdAt: Instant
		while (true) {
			when (val index = decodeElementIndex(descriptor)) {
				CompositeDecoder.DECODE_DONE -> break
				0 -> decodeStringElement(descriptor, index)
				1 -> createdAt = Instant.parse(decodeStringElement(descriptor, 1))
				else -> {
					val field = fields[index - 2]
					callback(field, index)
				}
			}
		}
		return createdAt
	}

	data class Field(
		val name: String,
		val descriptor: SerialDescriptor,
	)
}

object DeletedStateSerializer : StateSerializer<DeletedState>(DeletedState::class, Field("reason", String.serializer().descriptor)) {
	override fun CompositeEncoder.serializeAdditional(state: DeletedState, name: String, index: Int) = when (name) {
		"reason" -> encodeStringElement(descriptor, index, state.reason)
		else -> error("Unknown field: $name")
	}
	
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
	override fun CompositeEncoder.serializeAdditional(state: EnqueuedState, name: String, index: Int) = Unit
	
	override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
		EnqueuedState().apply { 
			createdAt = handleDeserialization { _, _ -> }
		}
	}
}

object FailedStateSerializer : StateSerializer<FailedState>(
	FailedState::class,
	Field("message", String.serializer().descriptor),
	Field("exceptionType", String.serializer().descriptor),
	Field("exceptionMessage", String.serializer().descriptor),
	Field("exceptionCauseType", String.serializer().descriptor),
	Field("exceptionCauseMessage", String.serializer().descriptor),
	Field("stackTrace", String.serializer().descriptor),
	Field("doNotRetry", Boolean.serializer().descriptor),
) {
	override fun CompositeEncoder.serializeAdditional(state: FailedState, name: String, index: Int) = when (name) {
		"message" -> encodeStringElement(descriptor, index, state.message)
		"exceptionType" -> encodeStringElement(descriptor, index, state.exceptionType)
		"exceptionMessage" -> encodeStringElement(descriptor, index, state.exceptionMessage)
		"exceptionCauseType" -> encodeNullableSerializableElement(descriptor, index, String.serializer(), state.exceptionCauseType)
		"exceptionCauseMessage" -> encodeNullableSerializableElement(descriptor, index, String.serializer(), state.exceptionCauseMessage)
		"stackTrace" -> encodeStringElement(descriptor, index, state.stackTrace)
		"doNotRetry" -> encodeBooleanElement(descriptor, index, state.mustNotRetry())
		else -> error("Unknown field: $name")
	}
	
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
	Field("serverId", Uuid.serializer().descriptor),
	Field("serverName", String.serializer().descriptor),
	Field("updatedAt", String.serializer().descriptor),
) {
	override fun CompositeEncoder.serializeAdditional(state: ProcessingState, name: String, index: Int) = when (name) {
		"serverId" -> encodeSerializableElement(descriptor, index, Uuid.serializer(), state.serverId.toKotlinUuid())
		"serverName" -> encodeStringElement(descriptor, index, state.serverName)
		"updatedAt" -> encodeStringElement(descriptor, index, state.updatedAt.toString())
		else -> error("Unknown field: $name")
	}

	override fun deserialize(decoder: Decoder): ProcessingState = decoder.decodeStructure(descriptor) {
		lateinit var serverId: Uuid
		lateinit var serverName: String
		lateinit var updatedAt: Instant
		val createdAt = handleDeserialization { field, index ->
			when (field.name) {
				"serverId" -> serverId = decodeSerializableElement(descriptor, index, Uuid.serializer())
				"serverName" -> serverName = decodeStringElement(descriptor, index)
				"updatedAt" -> updatedAt = Instant.parse(decodeStringElement(descriptor, index))
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
	Field("scheduledAt", String.serializer().descriptor),
	Field("reason", String.serializer().descriptor),
) {
	override fun CompositeEncoder.serializeAdditional(state: ScheduledState, name: String, index: Int) {
		when (name) {
			"scheduledAt" -> encodeStringElement(descriptor, index, state.scheduledAt.toString())
			"reason" -> encodeNullableSerializableElement(descriptor, index, String.serializer(), state.reason)
			else -> error("Unknown field: $name")
		}
	}
	
	override fun deserialize(decoder: Decoder): ScheduledState = decoder.decodeStructure(descriptor) {
		lateinit var scheduledAt: String
		var reason: String? = null
		val createdAt = handleDeserialization { field, index ->
			when (field.name) {
				"scheduledAt" -> scheduledAt = decodeStringElement(descriptor, index)
				"reason" -> reason = decodeNullableSerializableElement(descriptor, index, String.serializer())
				else -> error("Unknown field: ${field.name}")
			}
		}

		ScheduledState(Instant.parse(scheduledAt), reason).apply {
			this.createdAt = createdAt
		}
	}
}

object SucceededStateSerializer : StateSerializer<SucceededState>(
	SucceededState::class,
	Field("latencyDuration", DurationSerializer.descriptor),
	Field("processDuration", DurationSerializer.descriptor),
) {
	override fun CompositeEncoder.serializeAdditional(state: SucceededState, name: String, index: Int) = when (name) {
		"latencyDuration" -> encodeSerializableElement(descriptor, index, DurationSerializer, state.latencyDuration)
		"processDuration" -> encodeSerializableElement(descriptor, index, DurationSerializer, state.processDuration)
		else -> error("Unknown field: $name")
	}
	
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
