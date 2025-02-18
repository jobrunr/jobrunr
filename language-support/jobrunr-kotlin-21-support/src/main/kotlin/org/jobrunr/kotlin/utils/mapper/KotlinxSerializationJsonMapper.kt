package org.jobrunr.kotlin.utils.mapper

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.modules.*
import org.jobrunr.jobs.states.JobState
import org.jobrunr.kotlin.serialization.*
import org.jobrunr.utils.mapper.JobParameterJsonMapperException
import org.jobrunr.utils.mapper.JsonMapper
import java.io.OutputStream

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
}

@Suppress("UNCHECKED_CAST")
@ExperimentalSerializationApi
@InternalSerializationApi
class KotlinxSerializationJsonMapper(
	private val json: Json
) : JsonMapper {
	constructor(serializersModule: SerializersModule) : this(Json {
		encodeDefaults = true
		ignoreUnknownKeys = true
		
		classDiscriminator = "@class"
		
		this.serializersModule = serializersModule + jobRunrSerializersModule
	})
	
	constructor() : this(jobRunrSerializersModule)
	
	override fun serialize(obj: Any): String? = rethrowSerializationException {
		fun <T : Any> encode(obj: Any): String? {
			return json.encodeToString(json.serializersModule.serializer(obj::class) as SerializationStrategy<T>, obj as T)
		}

		encode<Any>(obj)
	}

	override fun serialize(outputStream: OutputStream, obj: Any) = rethrowSerializationException {
		fun <T : Any> encode(obj: Any, outputStream: OutputStream) {
			json.encodeToStream(
				json.serializersModule.serializer(obj::class) as SerializationStrategy<T>,
				obj as T,
				outputStream
			)
		}
		
		encode<Any>(obj, outputStream)
	}

	override fun <T : Any> deserialize(serializedObjectAsString: String?, clazz: Class<T>): T? = rethrowSerializationException {
		serializedObjectAsString?.let {
			json.decodeFromString(
				json.serializersModule.serializer(clazz.kotlin)!!,
				serializedObjectAsString,
			)
		}
	}

	fun <T> rethrowSerializationException(block: () -> T): T = try {
		block()
	} catch (e: SerializationException) {
		throw JobParameterJsonMapperException(e.message, e)
	}
}
