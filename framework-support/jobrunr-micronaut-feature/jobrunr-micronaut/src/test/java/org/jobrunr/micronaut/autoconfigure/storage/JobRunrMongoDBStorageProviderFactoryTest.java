package org.jobrunr.micronaut.autoconfigure.storage;

import com.mongodb.client.MongoClient;
import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;
import org.jobrunr.stubs.Mocks;
import org.junit.jupiter.api.Test;

import static org.jobrunr.micronaut.MicronautAssertions.assertThat;

@MicronautTest
class JobRunrMongoDBStorageProviderFactoryTest {

    @Inject
    ApplicationContext context;

    @Test
    void mongoDBStorageProviderAutoConfigurationTest() {
        assertThat(context).hasSingleBean(StorageProvider.class);
        assertThat(context.getBean(StorageProvider.class))
                .isInstanceOf(MongoDBStorageProvider.class)
                .hasJobMapper();
        assertThat(context).doesNotHaveBean(InMemoryStorageProvider.class);
    }

    @Singleton
    public MongoClient mongoClient() {
        return Mocks.mongoClient();
    }
}
