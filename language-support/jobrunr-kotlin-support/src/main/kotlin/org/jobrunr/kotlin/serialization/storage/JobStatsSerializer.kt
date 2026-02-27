package org.jobrunr.kotlin.serialization.storage

import kotlinx.serialization.builtins.serializer
import org.jobrunr.kotlin.serialization.misc.InstantSerializer
import org.jobrunr.kotlin.serialization.utils.FieldBasedSerializer
import org.jobrunr.kotlin.serialization.utils.FieldBasedSerializer.Field
import org.jobrunr.storage.JobStats

val JOB_STATS_SERIALIZER_FIELDS: List<Field<JobStats, out Any>> = listOf(
    Field("timeStamp", InstantSerializer) { it.timeStamp },
    Field("queryDurationInMillis", Long.serializer()) { it.queryDurationInMillis },
    Field("total", Long.serializer()) { it.total },
    Field("awaiting", Long.serializer()) { it.awaiting },
    Field("scheduled", Long.serializer()) { it.scheduled },
    Field("enqueued", Long.serializer()) { it.enqueued },
    Field("processing", Long.serializer()) { it.processing },
    Field("failed", Long.serializer()) { it.failed },
    Field("succeeded", Long.serializer()) { it.succeeded },
    Field("allTimeSucceeded", Long.serializer()) { it.allTimeSucceeded },
    Field("deleted", Long.serializer()) { it.deleted },
    Field("recurringJobs", Int.serializer()) { it.recurringJobs },
    Field("backgroundJobServers", Int.serializer()) { it.backgroundJobServers },
)

object JobStatsSerializer : FieldBasedSerializer<JobStats>(
    JobStats::class,
    JOB_STATS_SERIALIZER_FIELDS
)
