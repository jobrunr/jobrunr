package org.jobrunr.kotlin.serialization.storage

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import org.jobrunr.kotlin.serialization.misc.DurationSerializer
import org.jobrunr.kotlin.serialization.misc.InstantSerializer
import org.jobrunr.storage.BackgroundJobServerStatus
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
object BackgroundJobServerStatusSerializer : KSerializer<BackgroundJobServerStatus> {
	override val descriptor = buildClassSerialDescriptor("BackgroundJobServerStatus") {
		element("id", Uuid.serializer().descriptor)
		element("name", String.serializer().descriptor)
		element("workerPoolSize", Int.serializer().descriptor)
		element("pollIntervalInSeconds", Int.serializer().descriptor)
		element("deleteSucceededJobsAfter", DurationSerializer.descriptor)
		element("permanentlyDeleteDeletedJobsAfter", DurationSerializer.descriptor)
		element("firstHeartbeat", InstantSerializer.descriptor)
		element("lastHeartbeat", InstantSerializer.descriptor)
		element("running", Boolean.serializer().descriptor)
		element("systemTotalMemory", Long.serializer().descriptor)
		element("systemFreeMemory", Long.serializer().descriptor)
		element("systemCpuLoad", Double.serializer().descriptor)
		element("processMaxMemory", Long.serializer().descriptor)
		element("processFreeMemory", Long.serializer().descriptor)
		element("processAllocatedMemory", Long.serializer().descriptor)
		element("processCpuLoad", Double.serializer().descriptor)
	}

	override fun serialize(encoder: Encoder, value: BackgroundJobServerStatus) = encoder.encodeStructure(descriptor) {
		encodeStringElement(descriptor, 0, value.id.toString())
		encodeStringElement(descriptor, 1, value.name)
		encodeIntElement(descriptor, 2, value.workerPoolSize)
		encodeIntElement(descriptor, 3, value.pollIntervalInSeconds)
		encodeSerializableElement(descriptor, 4, DurationSerializer, value.deleteSucceededJobsAfter)
		encodeSerializableElement(descriptor, 5, DurationSerializer, value.permanentlyDeleteDeletedJobsAfter)
		encodeSerializableElement(descriptor, 6, InstantSerializer, value.firstHeartbeat)
		encodeSerializableElement(descriptor, 7, InstantSerializer, value.lastHeartbeat)
		encodeBooleanElement(descriptor, 8, value.isRunning)
		encodeLongElement(descriptor, 9, value.systemTotalMemory)
		encodeLongElement(descriptor, 10, value.systemFreeMemory)
		encodeDoubleElement(descriptor, 11, value.systemCpuLoad)
		encodeLongElement(descriptor, 12, value.processMaxMemory)
		encodeLongElement(descriptor, 13, value.processFreeMemory)
		encodeLongElement(descriptor, 14, value.processAllocatedMemory)
		encodeDoubleElement(descriptor, 15, value.processCpuLoad)
	}

	override fun deserialize(decoder: Decoder) = TODO("Not yet implemented")
}