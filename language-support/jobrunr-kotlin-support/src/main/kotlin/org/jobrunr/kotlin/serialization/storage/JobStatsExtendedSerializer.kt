package org.jobrunr.kotlin.serialization.storage

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import org.jobrunr.kotlin.serialization.misc.InstantSerializer
import org.jobrunr.kotlin.serialization.utils.FieldBasedSerializer
import org.jobrunr.storage.JobStatsExtended

@Suppress("UNCHECKED_CAST")
val JOB_STATS_EXTENDED_SERIALIZER_FIELDS = JOB_STATS_SERIALIZER_FIELDS
        as List<FieldBasedSerializer.Field<JobStatsExtended, out Any>>

@Suppress("UNCHECKED_CAST")
object JobStatsExtendedSerializer : FieldBasedSerializer<JobStatsExtended>(
    JobStatsExtended::class,
    JOB_STATS_EXTENDED_SERIALIZER_FIELDS + listOf(
        Field("amountSucceeded", Long.serializer()) { it.amountSucceeded },
        Field("amountFailed", Long.serializer()) { it.amountFailed },
        Field("estimation", EstimationSerializer) { it.estimation },
    )
) {
    override fun serialize(encoder: Encoder, value: JobStatsExtended) = encoder.encodeStructure(descriptor) {
        with(JobStatsSerializer) {
            continueEncode(value)
        }
        continueEncode(value)
    }

    object EstimationSerializer : FieldBasedSerializer<JobStatsExtended.Estimation>(
        JobStatsExtended.Estimation::class,
        Field("processingDone", Boolean.serializer()) { it.isProcessingDone },
        Field("estimatedProcessingTimeAvailable", Boolean.serializer()) { it.isEstimatedProcessingFinishedInstantAvailable },
        Field("estimatedProcessingFinishedAt", InstantSerializer, nullable = true) { it.estimatedProcessingFinishedAt },
    )
}
