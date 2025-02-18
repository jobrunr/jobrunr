package org.jobrunr.kotlin.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import org.jobrunr.jobs.context.JobDashboardProgressBar

object JobDashboardProgressSerializer : KSerializer<JobDashboardProgressBar.JobDashboardProgress> {
	override val descriptor = buildClassSerialDescriptor(JobDashboardProgressBar.JobDashboardProgress::class.qualifiedName!!) {
		element("totalAmount", Long.serializer().descriptor)
		element("succeededAmount", Long.serializer().descriptor)
		element("failedAmount", Long.serializer().descriptor)
	}

	override fun serialize(encoder: Encoder, value: JobDashboardProgressBar.JobDashboardProgress) = encoder.encodeStructure(descriptor) {
		encodeLongElement(descriptor, 0, value.totalAmount)
		encodeLongElement(descriptor, 1, value.succeededAmount)
		encodeLongElement(descriptor, 2, value.failedAmount)
	}

	override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
		var totalAmount = -1L
		var succeededAmount = 0L
		var failedAmount = 0L

		while (true) {
			when (val index = decodeElementIndex(descriptor)) {
				0 -> totalAmount = decodeLongElement(descriptor, 0)
				1 -> succeededAmount = decodeLongElement(descriptor, 1)
				2 -> failedAmount = decodeLongElement(descriptor, 2)
				CompositeDecoder.DECODE_DONE -> break
				else -> error("Unexpected index: $index")
			}
		}

		JobDashboardProgressBar.JobDashboardProgress(totalAmount).apply {
			setProgress(totalAmount, succeededAmount, failedAmount)
		}
	}
}