package org.jobrunr.kotlin.serialization

import kotlinx.serialization.Serializable
import org.jobrunr.jobs.JobDetails
import org.jobrunr.jobs.JobParameter

@Serializable
data class KJobDetails(
	val className: String,
	val staticFieldName: String?,
	val methodName: String,
	val jobParameters: List<@Serializable(with = JobParameterSerializer::class) JobParameter>,
	val cacheable: Boolean,
) {
	companion object : KSerializable<KJobDetails, JobDetails> {
		override fun mapToJava(kotlin: KJobDetails) = JobDetails(
			kotlin.className,
			kotlin.staticFieldName,
			kotlin.methodName,
			kotlin.jobParameters,
		).apply { 
			cacheable = kotlin.cacheable
		}

		override fun mapToKotlin(java: JobDetails) = KJobDetails(
			className = java.className,
			staticFieldName = java.staticFieldName,
			methodName = java.methodName,
			jobParameters = java.jobParameters,
			cacheable = java.cacheable,
		)
	}
}