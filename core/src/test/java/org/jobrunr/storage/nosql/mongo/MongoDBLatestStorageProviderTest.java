package org.jobrunr.storage.nosql.mongo;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

@Testcontainers
public class MongoDBLatestStorageProviderTest extends AbstractMongoDBStorageProviderTest {

    @Container
    private static final MongoDBContainer mongoContainer = new MongoDBContainer("mongo:latest").withExposedPorts(27017);

    @Override
    protected MongoDBContainer getMongoContainer() {
        return mongoContainer;
    }
}
