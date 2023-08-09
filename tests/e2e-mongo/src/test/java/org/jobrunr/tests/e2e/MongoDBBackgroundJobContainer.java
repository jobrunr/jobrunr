package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

public class MongoDBBackgroundJobContainer extends AbstractBackgroundJobContainer {

    private final GenericContainer mongoContainer;

    public MongoDBBackgroundJobContainer(GenericContainer mongoContainer, Network network) {
        super("jobrunr-e2e-mongo:1.0");
        this.mongoContainer = mongoContainer;
        this.withNetwork(network);
    }

    @Override
    public void start() {
        this
                .dependsOn(mongoContainer)
                .withEnv("MONGO_HOST", mongoContainer.getNetworkAliases().get(0).toString())
                .withEnv("MONGO_PORT", String.valueOf(27017));

        super.start();
    }

    @Override
    public StorageProvider getStorageProviderForClient() {
        return new MongoDBStorageProvider(mongoContainer.getHost(), mongoContainer.getFirstMappedPort());
    }

}
