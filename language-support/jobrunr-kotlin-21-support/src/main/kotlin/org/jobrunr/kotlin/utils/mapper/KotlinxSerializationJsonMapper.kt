package org.jobrunr.kotlin.utils.mapper

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import org.jobrunr.jobs.Job
import org.jobrunr.jobs.JobDetails
import org.jobrunr.jobs.JobParameter
import org.jobrunr.jobs.states.EnqueuedState
import org.jobrunr.jobs.states.JobState
import org.jobrunr.kotlin.serialization.*
import org.jobrunr.utils.mapper.JsonMapper
import java.io.OutputStream
import kotlin.reflect.KClass

@ExperimentalSerializationApi
@InternalSerializationApi
class KotlinxSerializationJsonMapper(
	private val json: Json = Json {
		encodeDefaults = true
		ignoreUnknownKeys = true
		
		this.serializersModule = SerializersModule {
			polymorphic(JobState::class) {
				subclass(EnqueuedState::class, EnqueuedStateSerializer)
			}
		}
	}
) : JsonMapper {
	@Suppress("UNCHECKED_CAST")
	private fun <T : Any> mapToKotlin(obj: Any): T {
		obj as T
		val mapped = (getSerializableClass(obj.javaClass) as KSerializable<*, T>?)
			?.mapToKotlin(obj) as T?

		return mapped ?: obj
	}
	
	@Suppress("UNCHECKED_CAST")
	override fun serialize(obj: Any): String? {
		fun <T : Any> encode(ogObj: Any): String? {
			val obj = mapToKotlin<T>(ogObj)

			return json.encodeToString((obj::class as KClass<T>).serializer(), obj)
		}

		return encode<Any>(obj)
	}

	override fun serialize(outputStream: OutputStream, `object`: Any) = json.encodeToStream(
		mapToKotlin(`object`),
		`object`,
		outputStream
	)

	override fun <T : Any> deserialize(serializedObjectAsString: String?, clazz: Class<T>): T? = serializedObjectAsString?.let {
		@Suppress("UNCHECKED_CAST")
		val serializable = getSerializableClass(clazz) as KSerializable<T, T>?
		val result = json.decodeFromString(
			serializable?.serializer ?: clazz.kotlin.serializer(),
			serializedObjectAsString,
		)
		serializable?.mapToJava(result) ?: result
	}
	
	fun getSerializableClass(clazz: Class<*>): KSerializable<*, *>? = when (clazz) {
		KJob::class.java, Job::class.java -> KJob
		KJobDetails::class.java, JobDetails::class.java -> KJobDetails
		KJobParameter::class.java, JobParameter::class.java -> KJobParameter
		else -> null
	}
}