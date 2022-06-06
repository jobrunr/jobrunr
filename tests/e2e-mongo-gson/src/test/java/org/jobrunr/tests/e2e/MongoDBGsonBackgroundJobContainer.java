package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

public class MongoDBGsonBackgroundJobContainer extends AbstractBackgroundJobContainer {

    private final GenericContainer mongoContainer;
    private final Network network;

    public MongoDBGsonBackgroundJobContainer(GenericContainer mongoContainer, Network network) {
        super("jobrunr-e2e-mongo-gson:1.0");
        this.mongoContainer = mongoContainer;
        this.network = network;
    }

    @Override
    public void start() {
        this
                .dependsOn(mongoContainer)
                .withNetwork(network)
                .withEnv("MONGO_HOST", "mongo")
                .withEnv("MONGO_PORT", String.valueOf(27017));

        super.start();
    }

    @Override
    public StorageProvider getStorageProviderForClient() {
        return new MongoDBStorageProvider(mongoContainer.getHost(), mongoContainer.getFirstMappedPort());
    }
}
