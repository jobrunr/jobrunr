package org.jobrunr.kotlin.serialization.utils

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

object ClassDiscriminatedContextualSerializer : KSerializer<Any> {
    @OptIn(InternalSerializationApi::class)
    override val descriptor = buildSerialDescriptor(Any::class.qualifiedName!!, SerialKind.CONTEXTUAL) {}

    override fun serialize(encoder: Encoder, value: Any) {
        require(encoder is JsonEncoder)

        @Suppress("UNCHECKED_CAST")
        val serializer = encoder.serializersModule.serializer(value::class) as KSerializer<Any>
        when (val jsonElement = encoder.json.encodeToJsonElement(serializer, value)) {
            is JsonObject -> {
                val jsonMap = jsonElement.jsonObject.toMutableMap()
                jsonMap["@class"] = JsonPrimitive(value::class.java.name)
                encoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(jsonMap))
            }

            is JsonArray -> {
                val jsonArray = mutableListOf<JsonElement>()
                jsonArray.add(0, JsonPrimitive(value::class.java.name))
                jsonArray.add(1, jsonElement)
                encoder.encodeSerializableValue(JsonArray.serializer(), JsonArray(jsonArray))
            }

            else -> {
                encoder.encodeJsonElement(jsonElement)
            }
        }
    }

    override fun deserialize(decoder: Decoder): Any {
        require(decoder is JsonDecoder)

        val jsonElement = decoder.decodeJsonElement()

        if (jsonElement is JsonObject && "@class" in jsonElement) {
            val serializer = decoder.serializersModule.serializer(Class.forName(jsonElement["@class"]!!.jsonPrimitive.content).kotlin)
            val elementToDecode = if ("value" in jsonElement) jsonElement["value"]!! else jsonElement

            return decoder.json.decodeFromJsonElement(serializer as DeserializationStrategy<Any>, elementToDecode)
        } else if (jsonElement is JsonArray) {
            val serializer = decoder.serializersModule.serializer(Class.forName(jsonElement[0].jsonPrimitive.content).kotlin)

            return decoder.json.decodeFromJsonElement(serializer as DeserializationStrategy<Any>, jsonElement[1])
        } else if (jsonElement is JsonPrimitive) {
            return when {
                jsonElement.isString -> jsonElement.content
                jsonElement.intOrNull != null -> jsonElement.int
                jsonElement.longOrNull != null -> jsonElement.long
                // why no float? double is the standard and float is almost never used
                jsonElement.doubleOrNull != null -> jsonElement.double
                jsonElement.booleanOrNull != null -> jsonElement.boolean
                else -> error("Unexpected json element found")
            }
        }

        error("Unexpected json element found")
    }

    interface PolymorphicContinuationSerializer<T> {
        fun CompositeEncoder.continueEncode(value: T)
    }

    interface PolymorphicContinuationDeserializer {
        fun CompositeDecoder.continueDecode(): Any
    }
}
