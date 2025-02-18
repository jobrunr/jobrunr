package org.jobrunr.kotlin.serialization

import kotlinx.serialization.Serializable
import org.jobrunr.jobs.Job
import org.jobrunr.jobs.states.JobState
import org.jobrunr.kotlin.utils.mapper.AnySerializer
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class KJob(
	val id: Uuid,
	val version: Int,
	val jobName: String,
	val amountOfRetries: Int?,
	val labels: Set<String>,
	val jobDetails: KJobDetails,
	val jobHistory: List<JobState>,
	val metadata: Map<String, @Serializable(with = AnySerializer::class) Any>,
	val recurringJobId: String?,
) {
	companion object : KSerializable<KJob, Job> {
		override fun mapToJava(kotlin: KJob) = Job(
			kotlin.id.toJavaUuid(),
			kotlin.version,
			KJobDetails.mapToJava(kotlin.jobDetails),
			kotlin.jobHistory,
			ConcurrentHashMap(kotlin.metadata),
		).apply {
			kotlin.recurringJobId?.let { setRecurringJobId(it) }
			jobName = kotlin.jobName
			amountOfRetries = kotlin.amountOfRetries
			labels = kotlin.labels
		}

		override fun mapToKotlin(java: Job) = KJob(
			id = java.id.toKotlinUuid(),
			version = java.version,
			jobName = java.jobName,
			amountOfRetries = java.amountOfRetries,
			labels = java.labels,
			jobDetails = KJobDetails.mapToKotlin(java.jobDetails),
			jobHistory = java.jobStates,
			metadata = java.metadata,
			recurringJobId = java.recurringJobId.getOrNull(),
		)
	}
}