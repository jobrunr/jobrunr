package org.jobrunr.kotlin.utils.mapper

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.modules.*
import kotlinx.serialization.serializer
import org.jobrunr.jobs.Job
import org.jobrunr.jobs.JobDetails
import org.jobrunr.jobs.RecurringJob
import org.jobrunr.jobs.states.JobState
import org.jobrunr.kotlin.serialization.*
import org.jobrunr.utils.mapper.JobParameterJsonMapperException
import org.jobrunr.utils.mapper.JsonMapper
import java.io.OutputStream
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
	contextual(JobParameterNotDeserializableExceptionSerializer)
	contextual(JobContextSerializer)
	contextual(JobDashboardLogLineSerializer)
	contextual(JobDashboardLogLinesSerializer)
	contextual(JobDashboardProgressSerializer)
}

@ExperimentalSerializationApi
@InternalSerializationApi
class KotlinxSerializationJsonMapper(
	private val json: Json
) : JsonMapper {
	constructor(serializersModule: SerializersModule) : this(Json {
		encodeDefaults = true
		ignoreUnknownKeys = true
		
		this.serializersModule = serializersModule + jobRunrSerializersModule
	})
	
	constructor() : this(jobRunrSerializersModule)
	
	@Suppress("UNCHECKED_CAST")
	private fun <T : Any> mapToKotlin(obj: Any): T {
		obj as T
		val mapped = (getMappingSerializable(obj.javaClass) as KSerializable<*, T>?)
			?.mapToKotlin(obj) as T?

		return mapped ?: obj
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun serialize(obj: Any): String? = rethrowSerializationException {
		fun <T : Any> encode(ogObj: Any): String? {
			val obj = mapToKotlin<T>(ogObj)

			return json.encodeToString((obj::class as KClass<T>).serializer(), obj)
		}

		encode<Any>(obj)
	}

	override fun serialize(outputStream: OutputStream, `object`: Any) = rethrowSerializationException {
		json.encodeToStream(
			mapToKotlin(`object`),
			`object`,
			outputStream
		)
	}

	override fun <T : Any> deserialize(serializedObjectAsString: String?, clazz: Class<T>): T? = rethrowSerializationException {
		serializedObjectAsString?.let {
			@Suppress("UNCHECKED_CAST")
			val serializable = getMappingSerializable(clazz) as KSerializable<T, T>?
			val result = json.decodeFromString(
				serializable?.serializer ?: clazz.kotlin.serializer(),
				serializedObjectAsString,
			)
			serializable?.mapToJava(result) ?: result
		}
	}
	
	private fun getMappingSerializable(clazz: Class<*>): KSerializable<*, *>? = when (clazz) {
		KJob::class.java, Job::class.java -> KJob
		KRecurringJob::class.java, RecurringJob::class.java -> KRecurringJob
		KJobDetails::class.java, JobDetails::class.java -> KJobDetails
		else -> null
	}

	fun <T> rethrowSerializationException(block: () -> T): T = try {
		block()
	} catch (e: SerializationException) {
		throw JobParameterJsonMapperException(e.message, e)
	}
}