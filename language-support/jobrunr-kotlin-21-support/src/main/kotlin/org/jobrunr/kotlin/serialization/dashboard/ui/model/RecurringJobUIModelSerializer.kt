package org.jobrunr.kotlin.serialization.dashboard.ui.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import org.jobrunr.dashboard.ui.model.RecurringJobUIModel
import org.jobrunr.kotlin.serialization.jobs.RecurringJobSerializer
import org.jobrunr.kotlin.serialization.misc.InstantSerializer
import org.jobrunr.kotlin.serialization.utils.DeserializationUnsupportedException

object RecurringJobUIModelSerializer : KSerializer<RecurringJobUIModel> {
    override val descriptor = buildClassSerialDescriptor(RecurringJobUIModel::class.qualifiedName!!) {
        RecurringJobSerializer.buildClassSerialDescriptorElements(this)
        element("nextRun", InstantSerializer.descriptor)
    }

    override fun serialize(encoder: Encoder, value: RecurringJobUIModel) = encoder.encodeStructure(descriptor) {
        with(RecurringJobSerializer) {
            continueEncode(value)
        }
        encodeSerializableElement(descriptor, descriptor.getElementIndex("nextRun"), InstantSerializer, value.nextRun)
    }

    override fun deserialize(decoder: Decoder) = throw DeserializationUnsupportedException()
}
