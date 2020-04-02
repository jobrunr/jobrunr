package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.testcontainers.containers.GenericContainer;

public abstract class AbstractBackgroundJobContainer extends GenericContainer {

    public AbstractBackgroundJobContainer(String dockerImageName) {
        super(dockerImageName);
    }

    public abstract StorageProvider getStorageProviderForClient();
}
