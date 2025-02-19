package org.jobrunr.kotlin.serialization.dashboard.ui.model.problems

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.ClassSerialDescriptorBuilder
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import org.jobrunr.dashboard.ui.model.problems.*
import org.jobrunr.kotlin.serialization.storage.JobRunrMetadataSerializer
import org.jobrunr.kotlin.serialization.utils.Field
import kotlin.reflect.KClass

fun ClassSerialDescriptorBuilder.abstractProblemElements() {
	element("type", String.serializer().descriptor)
}

abstract class ProblemSerializer<ProblemType : Problem>(
	kClass: KClass<ProblemType>,
	private vararg val fields: Field,
) : KSerializer<ProblemType> {
	override val descriptor = buildClassSerialDescriptor(kClass.qualifiedName!!) {
		abstractProblemElements()
		fields.forEach { 
			element(it.name, it.descriptor)
		}
	}

	override fun serialize(encoder: Encoder, value: ProblemType) = encoder.encodeStructure(descriptor) {
		encodeStringElement(descriptor, 0, value.type)
		fields.forEachIndexed { index, field ->
			serializeAdditional(value, field.name, index + 1)
		}
	}
	
	abstract fun CompositeEncoder.serializeAdditional(problem: ProblemType, name: String, index: Int)

	override fun deserialize(decoder: Decoder) = TODO("Not yet implemented")
}

object CpuAllocationIrregularityProblemSerializer : ProblemSerializer<CpuAllocationIrregularityProblem>(
	CpuAllocationIrregularityProblem::class,
	Field("cpuAllocationIrregularityMetadataSet", ListSerializer(JobRunrMetadataSerializer).descriptor),
) {
	override fun CompositeEncoder.serializeAdditional(problem: CpuAllocationIrregularityProblem, name: String, index: Int) {
		encodeSerializableElement(
			descriptor, index, ListSerializer(JobRunrMetadataSerializer),
			problem.cpuAllocationIrregularityMetadataSet
		)
	}
}

object PollIntervalInSecondsTimeBoxIsTooSmallProblemSerializer : ProblemSerializer<PollIntervalInSecondsTimeBoxIsTooSmallProblem>(
	PollIntervalInSecondsTimeBoxIsTooSmallProblem::class,
	Field("pollIntervalInSecondsTimeBoxIsTooSmallMetadataSet", ListSerializer(JobRunrMetadataSerializer).descriptor),
) {
	override fun CompositeEncoder.serializeAdditional(problem: PollIntervalInSecondsTimeBoxIsTooSmallProblem, name: String, index: Int) {
		encodeSerializableElement(
			descriptor, index, ListSerializer(JobRunrMetadataSerializer),
			problem.pollIntervalInSecondsTimeBoxIsTooSmallMetadataSet
		)
	}
}

object ScheduledJobsNotFoundProblemSerializer : ProblemSerializer<ScheduledJobsNotFoundProblem>(
	ScheduledJobsNotFoundProblem::class,
	Field("jobsNotFound", SetSerializer(String.serializer()).descriptor),
) {
	override fun CompositeEncoder.serializeAdditional(problem: ScheduledJobsNotFoundProblem, name: String, index: Int) {
		encodeSerializableElement(descriptor, index, SetSerializer(String.serializer()), problem.jobsNotFound)
	}
}

object SevereJobRunrExceptionProblemSerializer : ProblemSerializer<SevereJobRunrExceptionProblem>(
	SevereJobRunrExceptionProblem::class,
	Field("githubIssueTitle", String.serializer().descriptor),
	Field("githubIssueBody", String.serializer().descriptor),
	Field("githubIssueBodyLength", Int.serializer().descriptor),
) {
	override fun CompositeEncoder.serializeAdditional(problem: SevereJobRunrExceptionProblem, name: String, index: Int) {
		when (name) {
			"githubIssueTitle" -> encodeStringElement(descriptor, index, problem.githubIssueTitle)
			"githubIssueBody" -> encodeStringElement(descriptor, index, problem.githubIssueBody)
			"githubIssueBodyLength" -> encodeIntElement(descriptor, index, problem.githubIssueBodyLength)
		}
	}
}