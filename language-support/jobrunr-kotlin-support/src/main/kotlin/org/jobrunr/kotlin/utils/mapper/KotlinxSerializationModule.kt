package org.jobrunr.kotlin.utils.mapper

import kotlinx.serialization.PolymorphicSerializer
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
import org.jobrunr.kotlin.serialization.jobs.states.KCarbonAwareAwaitingState
import org.jobrunr.kotlin.serialization.jobs.states.KDeletedState
import org.jobrunr.kotlin.serialization.jobs.states.KEnqueuedState
import org.jobrunr.kotlin.serialization.jobs.states.KFailedState
import org.jobrunr.kotlin.serialization.jobs.states.KProcessingState
import org.jobrunr.kotlin.serialization.jobs.states.KScheduledState
import org.jobrunr.kotlin.serialization.jobs.states.KSucceededState
import org.jobrunr.kotlin.serialization.misc.DurationSerializer
import org.jobrunr.kotlin.serialization.misc.InstantSerializer
import org.jobrunr.kotlin.serialization.misc.LocalDateTimeSerializer
import org.jobrunr.kotlin.serialization.misc.OffsetDateTimeSerializer
import org.jobrunr.kotlin.serialization.misc.QueueSerializer
import org.jobrunr.kotlin.serialization.misc.UUIDSerializer
import org.jobrunr.kotlin.serialization.server.carbonaware.CarbonIntensityForecastSerializer
import org.jobrunr.kotlin.serialization.storage.BackgroundJobServerStatusSerializer
import org.jobrunr.kotlin.serialization.storage.JobRunrMetadataSerializer
import org.jobrunr.kotlin.serialization.storage.JobStatsExtendedSerializer
import org.jobrunr.kotlin.serialization.storage.JobStatsSerializer
import org.jobrunr.kotlin.serialization.storage.PageSerializer
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
        subclass(KCarbonAwareAwaitingState.Serializer)
        subclass(KScheduledState.Serializer)
        subclass(KSucceededState.Serializer)
    }
    contextual(JobSerializer)
    contextual(RecurringJobSerializer)
    contextual(JobContextSerializer)
    contextual(JobDashboardLogLineSerializer)
    contextual(JobDashboardLogLinesSerializer)
    contextual(JobDashboardProgressSerializer)
    contextual(CarbonIntensityForecastSerializer)

    contextual(JobStatsSerializer)
    contextual(JobStatsExtendedSerializer)
    contextual(BackgroundJobServerStatusSerializer)
    contextual(RecurringJobUIModelSerializer)
    contextual(VersionUIModelSerializer)
    contextual(JobRunrMetadataSerializer)
    @Suppress("UNCHECKED_CAST")
    contextual(Problems::class as KClass<Queue<Problem>>, QueueSerializer(PolymorphicSerializer(Problem::class)))
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