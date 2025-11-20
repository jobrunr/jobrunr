package org.jobrunr.kotlin.utils.mapper

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.File

object FileSerializer : KSerializer<File> {
    override val descriptor = PrimitiveSerialDescriptor(File::class.qualifiedName!!, PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: File) = encoder.encodeString(value.absolutePath)

    override fun deserialize(decoder: Decoder): File = File(decoder.decodeString())
}
