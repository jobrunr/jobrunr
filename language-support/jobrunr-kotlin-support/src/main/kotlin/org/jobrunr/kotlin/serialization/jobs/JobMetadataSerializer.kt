package org.jobrunr.kotlin.serialization.jobs

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.jobrunr.kotlin.serialization.utils.ClassDiscriminatedContextualSerializer

object JobMetadataSerializer : KSerializer<Map<String, Any>> {
    override val descriptor = MapSerializer(String.serializer(), ClassDiscriminatedContextualSerializer).descriptor

    override fun serialize(encoder: Encoder, value: Map<String, Any>) {
        require(encoder is JsonEncoder)

        val map = encoder.json.encodeToJsonElement(MapSerializer(String.serializer(), ClassDiscriminatedContextualSerializer), value).jsonObject.toMutableMap()
        map["@class"] = JsonPrimitive(value::class.java.name)

        encoder.encodeSerializableValue(JsonObject.serializer(), JsonObject(map))
    }

    override fun deserialize(decoder: Decoder): Map<String, Any> {
        require(decoder is JsonDecoder)

        val map = decoder.decodeJsonElement().jsonObject.toMutableMap()
        map.remove("@class")

        return decoder.json.decodeFromJsonElement(MapSerializer(String.serializer(), ClassDiscriminatedContextualSerializer), JsonObject(map))
    }
}
