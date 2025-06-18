package org.jobrunr.kotlin.serialization.server.carbonaware

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import org.jobrunr.kotlin.serialization.misc.DurationSerializer
import org.jobrunr.kotlin.serialization.misc.InstantSerializer
import org.jobrunr.server.carbonaware.CarbonIntensityForecast
import org.jobrunr.server.carbonaware.CarbonIntensityForecast.ApiResponseStatus
import org.jobrunr.server.carbonaware.CarbonIntensityForecast.TimestampedCarbonIntensityForecast
import java.time.Duration
import java.time.Instant

object CarbonIntensityForecastSerializer : KSerializer<CarbonIntensityForecast> {
    override val descriptor = buildClassSerialDescriptor(CarbonIntensityForecast::class.qualifiedName!!) {
        element("apiResponse", ApiResponseStatusSerializer.descriptor)
        element("dataProvider", String.serializer().nullable.descriptor)
        element("dataIdentifier", String.serializer().nullable.descriptor)
        element("displayName", String.serializer().nullable.descriptor)
        element("timezone", String.serializer().nullable.descriptor)
        element("nextForecastAvailableAt", InstantSerializer.nullable.descriptor)
        element("forecastInterval", DurationSerializer.nullable.descriptor)
        element("intensityForecast", ListSerializer(TimestampedCarbonIntensityForecastSerializer).descriptor)
    }

    override fun serialize(encoder: Encoder, value: CarbonIntensityForecast) {
        SerializationException("Serialization for ${descriptor.serialName} is unsupported!")
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): CarbonIntensityForecast = decoder.decodeStructure(descriptor) {
        lateinit var apiResponse: ApiResponseStatus
        var dataProvider: String? = null
        var dataIdentifier: String? = null
        var displayName: String? = null
        var timeZone: String? = null
        var nextForecastAvailableAt: Instant? = null
        var forecastInterval: Duration? = null
        var intensityForecast: List<TimestampedCarbonIntensityForecast>? = null

        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> apiResponse = decodeSerializableElement(descriptor, index, ApiResponseStatusSerializer)
                1 -> dataProvider = decodeNullableSerializableElement(descriptor, index, String.serializer().nullable)
                2 -> dataIdentifier = decodeNullableSerializableElement(descriptor, index, String.serializer().nullable)
                3 -> displayName = decodeNullableSerializableElement(descriptor, index, String.serializer().nullable)
                4 -> timeZone = decodeNullableSerializableElement(descriptor, index, String.serializer().nullable)
                5 -> nextForecastAvailableAt = decodeNullableSerializableElement(descriptor, index, InstantSerializer)
                6 -> forecastInterval = decodeNullableSerializableElement(descriptor, index, DurationSerializer)
                7 -> intensityForecast = decodeNullableSerializableElement(descriptor, index, ListSerializer(TimestampedCarbonIntensityForecastSerializer))
                else -> error("Unexpected index $index")
            }
        }

        CarbonIntensityForecast(apiResponse, dataProvider, dataIdentifier, displayName, timeZone, nextForecastAvailableAt, forecastInterval, intensityForecast)
    }
}

object TimestampedCarbonIntensityForecastSerializer : KSerializer<TimestampedCarbonIntensityForecast> {
    override val descriptor = buildClassSerialDescriptor(TimestampedCarbonIntensityForecast::class.qualifiedName!!) {
        element("periodStartAt", InstantSerializer.descriptor)
        element("periodEndAt", InstantSerializer.descriptor)
        element("rank", Int.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: TimestampedCarbonIntensityForecast) {
        SerializationException("Serialization for ${descriptor.serialName} is unsupported!")
    }

    override fun deserialize(decoder: Decoder): TimestampedCarbonIntensityForecast = decoder.decodeStructure(descriptor) {
        lateinit var periodStartAt: Instant
        lateinit var periodEndAt: Instant
        var rank = 0

        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> periodStartAt = decodeSerializableElement(descriptor, 0, InstantSerializer)
                1 -> periodEndAt = decodeSerializableElement(descriptor, 1, InstantSerializer)
                2 -> rank = decodeIntElement(descriptor, 2)
                else -> error("Unexpected index $index")
            }
        }

        TimestampedCarbonIntensityForecast(periodStartAt, periodEndAt, rank)
    }
}

object ApiResponseStatusSerializer : KSerializer<ApiResponseStatus> {
    override val descriptor = buildClassSerialDescriptor(ApiResponseStatus::class.qualifiedName!!) {
        element("code", String.serializer().descriptor)
        element("message", String.serializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: ApiResponseStatus) {
        SerializationException("Serialization for ${descriptor.serialName} is unsupported!")
    }

    override fun deserialize(decoder: Decoder): ApiResponseStatus = decoder.decodeStructure(descriptor) {
        lateinit var code: String
        lateinit var message: String

        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break
                0 -> code = decodeStringElement(descriptor, 0)
                1 -> message = decodeStringElement(descriptor, 1)
                else -> error("Unexpected index $index")
            }
        }

        ApiResponseStatus(code, message)
    }
}