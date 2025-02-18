package org.jobrunr.kotlin.serialization

import kotlinx.serialization.Serializable
import org.jobrunr.jobs.JobParameter
import org.jobrunr.kotlin.utils.mapper.AnySerializer

@Serializable
data class KJobParameter(
	val className: String,
	val actualClassName: String,
	val `object`: @Serializable(with = AnySerializer::class) Any? = null,
) {
	companion object : KSerializable<KJobParameter, JobParameter> {
		override fun mapToJava(kotlin: KJobParameter) = JobParameter(
			kotlin.className,
			kotlin.actualClassName,
			kotlin.`object`
		)

		override fun mapToKotlin(java: JobParameter) = KJobParameter(
			className = java.className,
			actualClassName = java.actualClassName,
			`object` = java.`object`
		)
	}
}