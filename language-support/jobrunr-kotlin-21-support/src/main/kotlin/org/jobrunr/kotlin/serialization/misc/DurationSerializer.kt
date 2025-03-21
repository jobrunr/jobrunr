package org.jobrunr.kotlin.serialization.misc

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonUnquotedLiteral
import java.math.BigDecimal
import java.time.Duration

object DurationSerializer : KSerializer<Duration> {
    override val descriptor = PrimitiveSerialDescriptor(Duration::class.qualifiedName!!, PrimitiveKind.DOUBLE)

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Duration) {
        val valueFormatted = "${value.seconds}.${"%09d".format(value.nano)}"
        (encoder as JsonEncoder).encodeJsonElement(JsonUnquotedLiteral(valueFormatted))
    }

    override fun deserialize(decoder: Decoder): Duration {
        val durationAsSecAndNanoSec = BigDecimal(decoder.decodeDouble())
        return Duration.ofSeconds(
            durationAsSecAndNanoSec.toLong(),
            durationAsSecAndNanoSec.remainder(BigDecimal.ONE).movePointRight(9).abs().toLong()
        )
    }
}
