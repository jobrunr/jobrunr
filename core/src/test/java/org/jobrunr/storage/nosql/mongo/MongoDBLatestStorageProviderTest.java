package org.jobrunr.storage.nosql.mongo;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class MongoDBLatestStorageProviderTest extends AbstractMongoDBStorageProviderTest {

    @Container
    private static final GenericContainer mongoContainer = new GenericContainer("mongo:latest").withExposedPorts(27017);

    @Override
    protected GenericContainer getMongoContainer() {
        return mongoContainer;
    }
}
