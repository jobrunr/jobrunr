package org.jobrunr.kotlin.utils.mapper

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import org.jobrunr.kotlin.serialization.utils.serializer
import org.jobrunr.utils.mapper.JobParameterJsonMapperException
import org.jobrunr.utils.mapper.JsonMapper
import org.slf4j.LoggerFactory
import java.io.OutputStream
import kotlin.reflect.KClass

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

    private fun noSerializerFound(kClass: KClass<*>): Exception = JobParameterJsonMapperException("No serializer for ${kClass.qualifiedName}")

    private fun <T> rethrowSerializationException(block: () -> T): T = try {
        block()
    } catch (e: Exception) {
        LOGGER.error("An unexpected error occurred during serialization - please report this!", e)
        throw when (e) {
            is SerializationException -> JobParameterJsonMapperException(e.message, e)
            else -> e
        }
    }
}
