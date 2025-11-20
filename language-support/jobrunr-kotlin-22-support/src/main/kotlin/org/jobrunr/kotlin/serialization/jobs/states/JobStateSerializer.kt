package org.jobrunr.kotlin.serialization.jobs.states

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.jobrunr.jobs.states.CarbonAwareAwaitingState
import org.jobrunr.jobs.states.DeletedState
import org.jobrunr.jobs.states.EnqueuedState
import org.jobrunr.jobs.states.FailedState
import org.jobrunr.jobs.states.ProcessingState
import org.jobrunr.jobs.states.ScheduledState
import org.jobrunr.jobs.states.StateName
import org.jobrunr.jobs.states.SucceededState
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
) : KSerializer<Java> {
    override val descriptor = SerialDescriptor(javaClass.qualifiedName!!, kDTOSerializer.descriptor)

    abstract fun Java.toDTO(): Kotlin
    override fun serialize(encoder: Encoder, value: Java) {
        require(encoder is JsonEncoder)

        val kotlinValue = value.toDTO()

        val jsonObjectMap = encoder.json.encodeToJsonElement(kDTOSerializer, kotlinValue).jsonObject.toMutableMap()

        jsonObjectMap.entries.removeIf { it.value is JsonNull }

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
    val reason: String,
    val createdAt: @Serializable(with = InstantSerializer::class) Instant,
) {
    object Serializer : DTOSerializer<DeletedState, KDeletedState>(DeletedState::class, serializer()) {
        override fun DeletedState.toDTO() = KDeletedState(
            reason = reason,
            createdAt = createdAt,
        )

        override fun KDeletedState.fromDTO() = DeletedState(reason, createdAt)
    }
}

@Serializable
data class KCarbonAwareAwaitingState(
    val state: StateName = StateName.AWAITING,
    @Serializable(with = InstantSerializer::class) val preferredInstant: Instant? = null,
    @Serializable(with = InstantSerializer::class) val from: Instant? = null,
    @Serializable(with = InstantSerializer::class) val to: Instant? = null,
    val reason: String? = null,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
) {
    object Serializer : DTOSerializer<CarbonAwareAwaitingState, KCarbonAwareAwaitingState>(CarbonAwareAwaitingState::class, serializer()) {
        override fun CarbonAwareAwaitingState.toDTO() = KCarbonAwareAwaitingState(
            preferredInstant = preferredInstant,
            from = from,
            to = to,
            reason = reason,
            createdAt = createdAt,
        )

        override fun KCarbonAwareAwaitingState.fromDTO() = CarbonAwareAwaitingState(
            preferredInstant,
            from,
            to,
            reason,
            createdAt,
        )
    }
}

@Serializable
data class KScheduledState(
    val state: StateName = StateName.SCHEDULED,
    @Serializable(with = InstantSerializer::class) val scheduledAt: Instant,
    val reason: String? = null,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
) {
    object Serializer : DTOSerializer<ScheduledState, KScheduledState>(ScheduledState::class, serializer()) {
        override fun ScheduledState.toDTO() = KScheduledState(
            scheduledAt = scheduledAt,
            reason = reason,
            createdAt = createdAt,
        )

        override fun KScheduledState.fromDTO() = ScheduledState(
            scheduledAt,
            reason,
            createdAt,
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
    val serverId: @Serializable(with = UUIDSerializer::class) UUID,
    val serverName: String? = null,
    val createdAt: @Serializable(with = InstantSerializer::class) Instant,
    val updatedAt: @Serializable(with = InstantSerializer::class) Instant,
) {
    object Serializer : DTOSerializer<ProcessingState, KProcessingState>(ProcessingState::class, serializer()) {
        override fun ProcessingState.toDTO() = KProcessingState(
            serverId = serverId,
            serverName = serverName,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

        override fun KProcessingState.fromDTO() = ProcessingState(
            serverId,
            serverName,
            createdAt,
            updatedAt,
        )
    }
}

@Serializable
data class KSucceededState(
    val state: StateName = StateName.SUCCEEDED,
    val latencyDuration: @Serializable(with = DurationSerializer::class) Duration,
    val processDuration: @Serializable(with = DurationSerializer::class) Duration,
    val createdAt: @Serializable(with = InstantSerializer::class) Instant,
) {
    object Serializer : DTOSerializer<SucceededState, KSucceededState>(SucceededState::class, serializer()) {
        override fun SucceededState.toDTO() = KSucceededState(
            latencyDuration = latencyDuration,
            processDuration = processDuration,
            createdAt = createdAt,
        )

        override fun KSucceededState.fromDTO() = SucceededState(
            latencyDuration,
            processDuration,
            createdAt,
        )
    }
}

@Serializable
data class KFailedState(
    val state: StateName = StateName.FAILED,
    val message: String,
    val exceptionType: String,
    val exceptionMessage: String? = null,
    val exceptionCauseType: String? = null,
    val exceptionCauseMessage: String? = null,
    val stackTrace: String,
    val doNotRetry: Boolean = false,
    val createdAt: @Serializable(with = InstantSerializer::class) Instant,
) {
    object Serializer : DTOSerializer<FailedState, KFailedState>(FailedState::class, serializer()) {
        override fun FailedState.toDTO() = KFailedState(
            message = message,
            exceptionType = exceptionType,
            exceptionMessage = exceptionMessage,
            exceptionCauseType = exceptionCauseType,
            exceptionCauseMessage = exceptionCauseMessage,
            stackTrace = stackTrace,
            doNotRetry = mustNotRetry(),
            createdAt = createdAt,
        )

        override fun KFailedState.fromDTO() = FailedState(
            message,
            exceptionType,
            exceptionMessage,
            exceptionCauseType,
            exceptionCauseMessage,
            stackTrace,
            doNotRetry,
            createdAt,
        )
    }
}