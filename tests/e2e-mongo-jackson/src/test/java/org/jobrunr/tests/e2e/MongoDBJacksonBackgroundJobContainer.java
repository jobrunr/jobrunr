package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;

public class MongoDBJacksonBackgroundJobContainer extends AbstractBackgroundJobContainer {

    private final GenericContainer mongoContainer;

    public MongoDBJacksonBackgroundJobContainer(GenericContainer mongoContainer) {
        super("jobrunr-e2e-mongo-jackson:1.0");
        this.mongoContainer = mongoContainer;
    }

    @Override
    public void start() {
        Testcontainers.exposeHostPorts(mongoContainer.getFirstMappedPort());
        this
                .dependsOn(mongoContainer)
                .withEnv("MONGO_HOST", "host.testcontainers.internal")
                .withEnv("MONGO_PORT", String.valueOf(mongoContainer.getMappedPort(27017)));

        super.start();
    }

    @Override
    public StorageProvider getStorageProviderForClient() {
        return new MongoDBStorageProvider(mongoContainer.getContainerIpAddress(), mongoContainer.getFirstMappedPort());
    }

}
