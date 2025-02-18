package org.jobrunr.kotlin.serialization

import kotlinx.serialization.Serializable
import org.jobrunr.jobs.JobDetails
import org.jobrunr.jobs.RecurringJob

@Serializable
data class KRecurringJob(
	val id: String,
	val version: Int,
	val jobName: String,
	val amountOfRetries: Int?,
	val labels: Set<String>,
	val jobDetails: @Serializable(with = JobDetailsSerializer::class) JobDetails,
	val scheduleExpression: String,
	val zoneId: String,
	val createdAt: String,
) {
	companion object : KSerializable<KRecurringJob, RecurringJob> {
		override fun mapToJava(kotlin: KRecurringJob) = RecurringJob(
			kotlin.id,
			kotlin.version,
			kotlin.jobDetails,
			kotlin.scheduleExpression,
			kotlin.zoneId,
			kotlin.createdAt
		).apply { 
			jobName = kotlin.jobName
			amountOfRetries = kotlin.amountOfRetries
			labels = kotlin.labels
		}

		override fun mapToKotlin(java: RecurringJob) = KRecurringJob(
			id = java.id,
			version = java.version,
			jobName = java.jobName,
			amountOfRetries = java.amountOfRetries,
			labels = java.labels,
			jobDetails = java.jobDetails,
			scheduleExpression = java.scheduleExpression,
			zoneId = java.zoneId,
			createdAt = java.createdAt.toString(),
		)
	}
}