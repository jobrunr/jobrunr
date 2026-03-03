package org.jobrunr.kotlin.serialization.misc

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.jsonPrimitive
import org.jobrunr.utils.DurationUtils.fromBigDecimal
import org.jobrunr.utils.DurationUtils.toBigDecimal
import java.time.Duration

object DurationSerializer : KSerializer<Duration> {
    override val descriptor = PrimitiveSerialDescriptor(Duration::class.qualifiedName!!, PrimitiveKind.DOUBLE)

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Duration) {
        (encoder as JsonEncoder).encodeJsonElement(JsonUnquotedLiteral(toBigDecimal(value).toString()))
    }

    override fun deserialize(decoder: Decoder): Duration {
        val jsonElement = (decoder as JsonDecoder).decodeJsonElement()
        if (jsonElement is JsonPrimitive && jsonElement.isString) {
            return Duration.parse(jsonElement.content)
        }
        return fromBigDecimal(jsonElement.jsonPrimitive.content.toBigDecimal())
    }
}
