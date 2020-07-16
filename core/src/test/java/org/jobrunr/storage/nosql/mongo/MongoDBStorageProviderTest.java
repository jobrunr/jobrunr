package org.jobrunr.storage.nosql.mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderTest;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

@Testcontainers
public class MongoDBStorageProviderTest extends StorageProviderTest {

    @Container
    private static GenericContainer mongoContainer = new GenericContainer("mongo").withExposedPorts(27017);

    private static MongoClient mongoClient;

    @Override
    protected void cleanup() {
        final MongoDatabase jobrunrDb = mongoClient().getDatabase("jobrunr");
        jobrunrDb.drop();
    }

    @Override
    protected StorageProvider getStorageProvider() {
        final MongoDBStorageProvider dbStorageProvider = new MongoDBStorageProvider(mongoClient(), rateLimit().withoutLimits());
        dbStorageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        return dbStorageProvider;
    }

    @AfterAll
    public static void closeMongoClient() {
        mongoClient.close();
    }

    private MongoClient mongoClient() {
        if (mongoClient == null) {
            mongoClient = MongoClients.create(
                    MongoClientSettings.builder()
                            .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress(mongoContainer.getContainerIpAddress(), mongoContainer.getMappedPort(27017)))))
                            .build());
        }
        return mongoClient;
    }
}
