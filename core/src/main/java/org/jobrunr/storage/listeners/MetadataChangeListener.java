package org.jobrunr.storage.listeners;

import org.jobrunr.storage.JobRunrMetadata;

import java.util.List;

public interface MetadataChangeListener extends StorageProviderChangeListener {

    String listenForChangesOfMetadataName();

    void onChange(List<JobRunrMetadata> metadataList);

}
