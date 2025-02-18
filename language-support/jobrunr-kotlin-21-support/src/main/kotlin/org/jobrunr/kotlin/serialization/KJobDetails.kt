package org.jobrunr.kotlin.serialization

import kotlinx.serialization.Serializable
import org.jobrunr.jobs.JobDetails

@Serializable
data class KJobDetails(
	val className: String,
	val staticFieldName: String?,
	val methodName: String,
	val jobParameters: List<KJobParameter>,
	val cacheable: Boolean,
) {
	companion object : KSerializable<KJobDetails, JobDetails> {
		override fun mapToJava(kotlin: KJobDetails) = JobDetails(
			kotlin.className,
			kotlin.staticFieldName,
			kotlin.methodName,
			kotlin.jobParameters.map(KJobParameter::mapToJava),
		).apply { 
			cacheable = kotlin.cacheable
		}

		override fun mapToKotlin(java: JobDetails) = KJobDetails(
			className = java.className,
			staticFieldName = java.staticFieldName,
			methodName = java.methodName,
			jobParameters = java.jobParameters.map(KJobParameter::mapToKotlin),
			cacheable = java.cacheable,
		)
	}
}