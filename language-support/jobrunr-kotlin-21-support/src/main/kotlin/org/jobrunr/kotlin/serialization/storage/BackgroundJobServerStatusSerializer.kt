package org.jobrunr.kotlin.serialization.storage

import kotlinx.serialization.builtins.serializer
import org.jobrunr.kotlin.serialization.misc.DurationSerializer
import org.jobrunr.kotlin.serialization.misc.InstantSerializer
import org.jobrunr.kotlin.serialization.utils.FieldBasedSerializer
import org.jobrunr.storage.BackgroundJobServerStatus
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
object BackgroundJobServerStatusSerializer : FieldBasedSerializer<BackgroundJobServerStatus>(
	BackgroundJobServerStatus::class,
	Field("id", Uuid.serializer()) { it.id.toKotlinUuid() },
	Field("name", String.serializer()) { it.name },
	Field("workerPoolSize", Int.serializer()) { it.workerPoolSize },
	Field("pollIntervalInSeconds", Int.serializer()) { it.pollIntervalInSeconds },
	Field("deleteSucceededJobsAfter", DurationSerializer) { it.deleteSucceededJobsAfter },
	Field("permanentlyDeleteDeletedJobsAfter", DurationSerializer) { it.permanentlyDeleteDeletedJobsAfter },
	Field("firstHeartbeat", InstantSerializer) { it.firstHeartbeat },
	Field("lastHeartbeat", InstantSerializer) { it.lastHeartbeat },
	Field("running", Boolean.serializer()) { it.isRunning },
	Field("systemTotalMemory", Long.serializer()) { it.systemTotalMemory },
	Field("systemFreeMemory", Long.serializer()) { it.systemFreeMemory },
	Field("systemCpuLoad", Double.serializer()) { it.systemCpuLoad },
	Field("processMaxMemory", Long.serializer()) { it.processMaxMemory },
	Field("processFreeMemory", Long.serializer()) { it.processFreeMemory },
	Field("processAllocatedMemory", Long.serializer()) { it.processAllocatedMemory },
	Field("processCpuLoad", Double.serializer()) { it.processCpuLoad }
)
