package org.jobrunr.kotlin.serialization.dashboard.ui.model.problems

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import org.jobrunr.dashboard.ui.model.problems.CpuAllocationIrregularityProblem
import org.jobrunr.dashboard.ui.model.problems.PollIntervalInSecondsTimeBoxIsTooSmallProblem
import org.jobrunr.dashboard.ui.model.problems.Problem
import org.jobrunr.dashboard.ui.model.problems.ScheduledJobsNotFoundProblem
import org.jobrunr.dashboard.ui.model.problems.SevereJobRunrExceptionProblem
import org.jobrunr.kotlin.serialization.storage.JobRunrMetadataSerializer
import org.jobrunr.kotlin.serialization.utils.FieldBasedSerializer
import kotlin.reflect.KClass

abstract class ProblemSerializer<ProblemType : Problem>(
    kClass: KClass<ProblemType>,
    vararg fields: Field<ProblemType, out Any>,
) : FieldBasedSerializer<ProblemType>(
    kClass,
    listOf(
        Field("type", String.serializer()) { it.type },
        *fields
    )
)

object CpuAllocationIrregularityProblemSerializer : ProblemSerializer<CpuAllocationIrregularityProblem>(
    CpuAllocationIrregularityProblem::class,
    Field("cpuAllocationIrregularityMetadataSet", ListSerializer(JobRunrMetadataSerializer)) {
        it.cpuAllocationIrregularityMetadataSet
    },
)

object PollIntervalInSecondsTimeBoxIsTooSmallProblemSerializer : ProblemSerializer<PollIntervalInSecondsTimeBoxIsTooSmallProblem>(
    PollIntervalInSecondsTimeBoxIsTooSmallProblem::class,
    Field("pollIntervalInSecondsTimeBoxIsTooSmallMetadataSet", ListSerializer(JobRunrMetadataSerializer)) {
        it.pollIntervalInSecondsTimeBoxIsTooSmallMetadataSet
    },
)

object ScheduledJobsNotFoundProblemSerializer : ProblemSerializer<ScheduledJobsNotFoundProblem>(
    ScheduledJobsNotFoundProblem::class,
    Field("jobsNotFound", SetSerializer(String.serializer())) { it.jobsNotFound },
)

object SevereJobRunrExceptionProblemSerializer : ProblemSerializer<SevereJobRunrExceptionProblem>(
    SevereJobRunrExceptionProblem::class,
    Field("githubIssueTitle", String.serializer()) { it.githubIssueTitle },
    Field("githubIssueBody", String.serializer()) { it.githubIssueBody },
    Field("githubIssueBodyLength", Int.serializer()) { it.githubIssueBodyLength },
)
