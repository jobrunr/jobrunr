package org.jobrunr.kotlin.utils.mapper

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jobrunr.dashboard.ui.model.problems.Problem
import org.jobrunr.dashboard.ui.model.problems.Problems
import org.jobrunr.jobs.states.JobState
import org.jobrunr.kotlin.serialization.dashboard.ui.model.RecurringJobUIModelSerializer
import org.jobrunr.kotlin.serialization.dashboard.ui.model.VersionUIModelSerializer
import org.jobrunr.kotlin.serialization.dashboard.ui.model.problems.CpuAllocationIrregularityProblemSerializer
import org.jobrunr.kotlin.serialization.dashboard.ui.model.problems.PollIntervalInSecondsTimeBoxIsTooSmallProblemSerializer
import org.jobrunr.kotlin.serialization.dashboard.ui.model.problems.ScheduledJobsNotFoundProblemSerializer
import org.jobrunr.kotlin.serialization.dashboard.ui.model.problems.SevereJobRunrExceptionProblemSerializer
import org.jobrunr.kotlin.serialization.jobs.JobSerializer
import org.jobrunr.kotlin.serialization.jobs.RecurringJobSerializer
import org.jobrunr.kotlin.serialization.jobs.context.JobContextSerializer
import org.jobrunr.kotlin.serialization.jobs.context.JobDashboardLogLineSerializer
import org.jobrunr.kotlin.serialization.jobs.context.JobDashboardLogLinesSerializer
import org.jobrunr.kotlin.serialization.jobs.context.JobDashboardProgressSerializer
import org.jobrunr.kotlin.serialization.jobs.states.*
import org.jobrunr.kotlin.serialization.misc.*
import org.jobrunr.kotlin.serialization.storage.*
import org.jobrunr.kotlin.serialization.utils.AnyInlineSerializer
import org.jobrunr.kotlin.serialization.utils.ClassDiscriminatedContextualSerializer
import java.util.*
import kotlin.reflect.KClass

internal val jobRunrSerializersModule = SerializersModule {
    polymorphic(JobState::class) {
        subclass(KDeletedState.Serializer)
        subclass(KEnqueuedState.Serializer)
        subclass(KFailedState.Serializer)
        subclass(KProcessingState.Serializer)
        subclass(KScheduledState.Serializer)
        subclass(KSucceededState.Serializer)
    }
    contextual(JobSerializer)
    contextual(RecurringJobSerializer)
    contextual(JobContextSerializer)
    contextual(JobDashboardLogLineSerializer)
    contextual(JobDashboardLogLinesSerializer)
    contextual(JobDashboardProgressSerializer)

    contextual(JobStatsSerializer)
    contextual(JobStatsExtendedSerializer)
    contextual(BackgroundJobServerStatusSerializer)
    contextual(RecurringJobUIModelSerializer)
    contextual(VersionUIModelSerializer)
    contextual(JobRunrMetadataSerializer)
    @Suppress("UNCHECKED_CAST")
    contextual(Problems::class as KClass<Queue<Problem>>, QueueSerializer(AnyInlineSerializer()))
    polymorphic(Problem::class) {
        subclass(CpuAllocationIrregularityProblemSerializer)
        subclass(PollIntervalInSecondsTimeBoxIsTooSmallProblemSerializer)
        subclass(ScheduledJobsNotFoundProblemSerializer)
        subclass(SevereJobRunrExceptionProblemSerializer)
    }
    contextual(PageSerializer<Any>())

    contextual(DurationSerializer)
    contextual(InstantSerializer)
    contextual(OffsetDateTimeSerializer)
    contextual(LocalDateTimeSerializer)

    contextual(FileSerializer)
    @Suppress("UNCHECKED_CAST")
    contextual(ArrayList::class as KClass<List<Any>>, ListSerializer(AnyInlineSerializer()))
    contextual(SetSerializer(ClassDiscriminatedContextualSerializer))
    contextual(UUIDSerializer)
}