@file:OptIn(ExperimentalSerializationApi::class)

package org.jobrunr.kotlin.serialization.jobs.states

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.jobrunr.jobs.states.*
import org.jobrunr.kotlin.serialization.misc.DurationSerializer
import org.jobrunr.kotlin.serialization.misc.InstantSerializer
import org.jobrunr.kotlin.serialization.misc.UUIDSerializer
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass

abstract class DTOSerializer<Java : Any, Kotlin : Any>(
    javaClass: KClass<Java>,
    private val kDTOSerializer: KSerializer<Kotlin>,
    private val patchJsonMap: (Kotlin, MutableMap<String, JsonElement>) -> Unit = { _, _ -> },
) : KSerializer<Java> {
    override val descriptor = SerialDescriptor(javaClass.qualifiedName!!, kDTOSerializer.descriptor)

    abstract fun Java.toDTO(): Kotlin
    override fun serialize(encoder: Encoder, value: Java) {
        require(encoder is JsonEncoder)

        val kotlinValue = value.toDTO()

        val jsonObjectMap = encoder.json.encodeToJsonElement(kDTOSerializer, kotlinValue).jsonObject.toMutableMap()
        jsonObjectMap["@class"] = JsonPrimitive(value.javaClass.canonicalName)
        
        patchJsonMap(kotlinValue, jsonObjectMap)

        encoder.encodeJsonElement(JsonObject(jsonObjectMap))
    }

    abstract fun Kotlin.fromDTO(): Java
    override fun deserialize(decoder: Decoder): Java {
        val kotlinValue = decoder.decodeSerializableValue(kDTOSerializer)

        return kotlinValue.fromDTO()
    }
}

@Serializable
data class KDeletedState(
    val state: StateName = StateName.DELETED,
    val createdAt: @Serializable(with = InstantSerializer::class) Instant,
    val reason: String,
) {
    object Serializer : DTOSerializer<DeletedState, KDeletedState>(DeletedState::class, serializer()) {
        override fun DeletedState.toDTO() = KDeletedState(
            createdAt = createdAt,
            reason = reason,
        )

        override fun KDeletedState.fromDTO() = DeletedState(createdAt, reason)
    }
}

@Serializable
data class KScheduledState(
    val state: StateName = StateName.SCHEDULED,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val scheduledAt: Instant,
    val reason: String? = null,
) {
    object Serializer : DTOSerializer<ScheduledState, KScheduledState>(
        ScheduledState::class,
        serializer(),
        patchJsonMap = { it, map ->
            if (it.reason == null) map.remove(KScheduledState::reason.name)
        }
    ) {
        override fun ScheduledState.toDTO() = KScheduledState(
            createdAt = createdAt,
            scheduledAt = scheduledAt,
            reason = reason,
        )

        override fun KScheduledState.fromDTO() = ScheduledState(
            createdAt,
            scheduledAt,
            reason,
            null,
        )
    }
}

@Serializable
data class KEnqueuedState(
    val state: StateName = StateName.ENQUEUED,
    val createdAt: @Serializable(with = InstantSerializer::class) Instant,
) {
    object Serializer : DTOSerializer<EnqueuedState, KEnqueuedState>(EnqueuedState::class, serializer()) {
        override fun EnqueuedState.toDTO() = KEnqueuedState(createdAt = createdAt)

        override fun KEnqueuedState.fromDTO() = EnqueuedState(createdAt)
    }
}


@Serializable
data class KProcessingState(
    val state: StateName = StateName.PROCESSING,
    val createdAt: @Serializable(with = InstantSerializer::class) Instant,
    val serverId: @Serializable(with = UUIDSerializer::class) UUID,
    val serverName: String,
    val updatedAt: @Serializable(with = InstantSerializer::class) Instant,
) {
    object Serializer : DTOSerializer<ProcessingState, KProcessingState>(ProcessingState::class, serializer()) {
        override fun ProcessingState.toDTO() = KProcessingState(
            createdAt = createdAt,
            serverId = serverId,
            serverName = serverName,
            updatedAt = updatedAt,
        )

        override fun KProcessingState.fromDTO() = ProcessingState(
            createdAt,
            updatedAt,
            serverId,
            serverName,
        )
    }
}

@Serializable
data class KSucceededState(
    val state: StateName = StateName.SUCCEEDED,
    val createdAt: @Serializable(with = InstantSerializer::class) Instant,
    val latencyDuration: @Serializable(with = DurationSerializer::class) Duration,
    val processDuration: @Serializable(with = DurationSerializer::class) Duration,
) {
    object Serializer : DTOSerializer<SucceededState, KSucceededState>(SucceededState::class, serializer()) {
        override fun SucceededState.toDTO() = KSucceededState(
            createdAt = createdAt,
            latencyDuration = latencyDuration,
            processDuration = processDuration,
        )

        override fun KSucceededState.fromDTO() = SucceededState(
            createdAt,
            latencyDuration,
            processDuration,
        )
    }
}

@Serializable
data class KFailedState(
    val state: StateName = StateName.FAILED,
    val createdAt: @Serializable(with = InstantSerializer::class) Instant,
    val message: String,
    val exceptionType: String,
    val stackTrace: String,
    val doNotRetry: Boolean = false,
) {
    object Serializer : DTOSerializer<FailedState, KFailedState>(FailedState::class, serializer()) {
        override fun FailedState.toDTO() = KFailedState(
            createdAt = createdAt,
            message = message,
            exceptionType = exceptionType,
            stackTrace = stackTrace,
            doNotRetry = mustNotRetry(),
        )

        override fun KFailedState.fromDTO() = FailedState(
            createdAt,
            message,
            exceptionType,
            null,
            null,
            null,
            stackTrace,
            doNotRetry
        )
    }
}