package org.jobrunr.kotlin.serialization.misc

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.time.Duration

object DurationSerializer : KSerializer<Duration> {
	override val descriptor = PrimitiveSerialDescriptor(Duration::class.qualifiedName!!, PrimitiveKind.DOUBLE)

	override fun serialize(encoder: Encoder, value: Duration) =
			encoder.encodeDouble(BigDecimal.valueOf(value.toNanos()).scaleByPowerOfTen(-9).toDouble())

	override fun deserialize(decoder: Decoder): Duration {
		val durationAsSecAndNanoSec = BigDecimal.valueOf(decoder.decodeDouble())
		return Duration.ofSeconds(
			durationAsSecAndNanoSec.toLong(),
			durationAsSecAndNanoSec.remainder(BigDecimal.ONE).movePointRight(durationAsSecAndNanoSec.scale()).abs().toLong()
		)
	}
}
