package org.jobrunr.kotlin.utils.mapper

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.modules.*
import org.jobrunr.dashboard.ui.model.problems.Problem
import org.jobrunr.dashboard.ui.model.problems.Problems
import org.jobrunr.jobs.states.JobState
import org.jobrunr.kotlin.serialization.dashboard.ui.model.RecurringJobUIModelSerializer
import org.jobrunr.kotlin.serialization.dashboard.ui.model.VersionUIModelSerializer
import org.jobrunr.kotlin.serialization.dashboard.ui.model.problems.CpuAllocationIrregularityProblemSerializer
import org.jobrunr.kotlin.serialization.dashboard.ui.model.problems.PollIntervalInSecondsTimeBoxIsTooSmallProblemSerializer
import org.jobrunr.kotlin.serialization.dashboard.ui.model.problems.ScheduledJobsNotFoundProblemSerializer
import org.jobrunr.kotlin.serialization.dashboard.ui.model.problems.SevereJobRunrExceptionProblemSerializer
import org.jobrunr.kotlin.serialization.jobs.JobParameterNotDeserializableExceptionSerializer
import org.jobrunr.kotlin.serialization.jobs.JobSerializer
import org.jobrunr.kotlin.serialization.jobs.RecurringJobSerializer
import org.jobrunr.kotlin.serialization.jobs.context.JobContextSerializer
import org.jobrunr.kotlin.serialization.jobs.context.JobDashboardLogLineSerializer
import org.jobrunr.kotlin.serialization.jobs.context.JobDashboardLogLinesSerializer
import org.jobrunr.kotlin.serialization.jobs.context.JobDashboardProgressSerializer
import org.jobrunr.kotlin.serialization.jobs.states.*
import org.jobrunr.kotlin.serialization.storage.*
import org.jobrunr.kotlin.serialization.utils.AnyInlineSerializer
import org.jobrunr.kotlin.serialization.utils.DurationSerializer
import org.jobrunr.kotlin.serialization.utils.QueueSerializer
import org.jobrunr.kotlin.serialization.utils.serializer
import org.jobrunr.utils.mapper.JobParameterJsonMapperException
import org.jobrunr.utils.mapper.JsonMapper
import org.slf4j.LoggerFactory
import java.io.OutputStream
import java.util.*
import kotlin.reflect.KClass

private val jobRunrSerializersModule = SerializersModule {
	polymorphic(JobState::class) {
		subclass(DeletedStateSerializer)
		subclass(EnqueuedStateSerializer)
		subclass(FailedStateSerializer)
		subclass(ProcessingStateSerializer)
		subclass(ScheduledStateSerializer)
		subclass(SucceededStateSerializer)
	}
	contextual(JobSerializer)
	contextual(RecurringJobSerializer)
	contextual(JobParameterNotDeserializableExceptionSerializer)
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
	contextual(FileSerializer)
	@Suppress("UNCHECKED_CAST")
	contextual(ArrayList::class as KClass<List<Any>>, ListSerializer(AnyInlineSerializer()))
}

@Suppress("UNCHECKED_CAST")
@ExperimentalSerializationApi
@InternalSerializationApi
class KotlinxSerializationJsonMapper(
	private val json: Json
) : JsonMapper {
	companion object {
		private val LOGGER = LoggerFactory.getLogger(KotlinxSerializationJsonMapper::class.java)
	}

	constructor(serializersModule: SerializersModule) : this(Json {
		encodeDefaults = true
		ignoreUnknownKeys = true

		classDiscriminator = "@class"

		this.serializersModule = serializersModule + jobRunrSerializersModule
	})

	constructor() : this(jobRunrSerializersModule)

	override fun serialize(obj: Any): String? = rethrowSerializationException {
		fun <T : Any> encode(obj: Any): String? {
			return json.encodeToString(
				json.serializersModule.serializer(obj::class) as SerializationStrategy<T>?
					?: throw noSerializerFound(obj::class),
				obj as T
			)
		}

		encode<Any>(obj)
	}

	override fun serialize(outputStream: OutputStream, obj: Any) = rethrowSerializationException {
		fun <T : Any> encode(obj: Any, outputStream: OutputStream) {
			json.encodeToStream(
				json.serializersModule.serializer(obj::class) as SerializationStrategy<T>?
					?: throw noSerializerFound(obj::class),
				obj as T,
				outputStream
			)
		}

		encode<Any>(obj, outputStream)
	}

	override fun <T : Any> deserialize(serializedObjectAsString: String?, clazz: Class<T>): T? = rethrowSerializationException {
		serializedObjectAsString?.let {
			json.decodeFromString(
				json.serializersModule.serializer(clazz.kotlin)
					?: throw noSerializerFound(clazz.kotlin),
				serializedObjectAsString,
			)
		}
	}

	fun noSerializerFound(kClass: KClass<*>): Exception = JobParameterJsonMapperException("No serializer for ${kClass.qualifiedName}")

	fun <T> rethrowSerializationException(block: () -> T): T = try {
		block()
	} catch (e: Exception) {
		LOGGER.error("An unexpected error occurred during serialization - please report this!", e)
		throw when (e) {
			is SerializationException -> JobParameterJsonMapperException(e.message, e)
			else -> e
		}
	}
}
