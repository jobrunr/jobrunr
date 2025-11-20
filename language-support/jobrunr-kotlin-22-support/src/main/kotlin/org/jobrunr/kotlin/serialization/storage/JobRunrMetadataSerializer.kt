package org.jobrunr.kotlin.serialization.storage

import kotlinx.serialization.builtins.serializer
import org.jobrunr.kotlin.serialization.misc.InstantSerializer
import org.jobrunr.kotlin.serialization.utils.FieldBasedSerializer
import org.jobrunr.storage.JobRunrMetadata

object JobRunrMetadataSerializer : FieldBasedSerializer<JobRunrMetadata>(
    JobRunrMetadata::class,
    Field("name", String.serializer()) { it.name },
    Field("owner", String.serializer()) { it.owner },
    Field("createdAt", InstantSerializer) { it.createdAt },
    Field("updatedAt", InstantSerializer) { it.updatedAt },
    Field("value", String.serializer()) { it.value }
)
