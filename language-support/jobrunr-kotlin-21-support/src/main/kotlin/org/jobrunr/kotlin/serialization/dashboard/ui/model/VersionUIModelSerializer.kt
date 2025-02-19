package org.jobrunr.kotlin.serialization.dashboard.ui.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import org.jobrunr.dashboard.ui.model.VersionUIModel

object VersionUIModelSerializer : KSerializer<VersionUIModel> {
	override val descriptor = buildClassSerialDescriptor(VersionUIModel::class.qualifiedName!!) {
		element("version", String.serializer().descriptor)
		element("allowAnonymousDataUsage", Boolean.serializer().descriptor)
		element("clusterId", String.serializer().descriptor)
	}

	override fun serialize(encoder: Encoder, value: VersionUIModel) = encoder.encodeStructure(descriptor) {
		encodeStringElement(descriptor, 0, value.version)
		encodeBooleanElement(descriptor, 1, value.isAllowAnonymousDataUsage)
		encodeStringElement(descriptor, 2, value.clusterId)
	}

	override fun deserialize(decoder: Decoder) = TODO("Not yet implemented")
}