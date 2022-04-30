package org.jobrunr.storage.nosql.mongo;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

@Testcontainers
public class MongoDBLatestStorageProviderWithTablePrefixTest extends AbstractMongoDBStorageProviderTest {

    @Container
    private static final GenericContainer mongoContainer = new GenericContainer("mongo:latest").withExposedPorts(27017);

    @Override
    protected GenericContainer getMongoContainer() {
        return mongoContainer;
    }

    @Override
    protected void cleanup() {
        cleanup("some_other_db");
    }

    @Override
    protected StorageProvider getStorageProvider() {
        final MongoDBStorageProvider dbStorageProvider = new MongoDBStorageProvider(mongoClient(), "some_other_db", "some_prefix", StorageProviderUtils.DatabaseOptions.CREATE, rateLimit().withoutLimits());
        dbStorageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        return dbStorageProvider;
    }
}
