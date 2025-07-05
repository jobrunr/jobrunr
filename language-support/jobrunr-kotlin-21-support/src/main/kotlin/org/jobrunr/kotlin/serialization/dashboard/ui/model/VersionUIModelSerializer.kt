package org.jobrunr.kotlin.serialization.dashboard.ui.model

import kotlinx.serialization.builtins.serializer
import org.jobrunr.dashboard.ui.model.VersionUIModel
import org.jobrunr.kotlin.serialization.utils.FieldBasedSerializer

object VersionUIModelSerializer : FieldBasedSerializer<VersionUIModel>(
    VersionUIModel::class,
    Field("version", String.serializer()) { it.version },
    Field("allowAnonymousDataUsage", Boolean.serializer()) { it.isAllowAnonymousDataUsage },
    Field("clusterId", String.serializer(), true) { it.clusterId },
    Field("storageProviderType", String.serializer(), true) { it.storageProviderType },
)
