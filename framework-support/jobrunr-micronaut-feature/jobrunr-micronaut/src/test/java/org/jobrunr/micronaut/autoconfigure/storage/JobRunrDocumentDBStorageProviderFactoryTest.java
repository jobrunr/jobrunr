package org.jobrunr.micronaut.autoconfigure.storage;

import com.mongodb.client.MongoClient;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.documentdb.AmazonDocumentDBStorageProvider;
import org.jobrunr.stubs.Mocks;
import org.junit.jupiter.api.Test;

import static org.jobrunr.micronaut.MicronautAssertions.assertThat;

@MicronautTest
@Property(name = "jobrunr.database.type", value = "documentdb")
class JobRunrDocumentDBStorageProviderFactoryTest {
    @Inject
    ApplicationContext context;

    @Test
    void documentDBStorageProviderAutoConfigurationTest() {
        assertThat(context).hasSingleBean(StorageProvider.class);
        assertThat(context.getBean(StorageProvider.class))
                .isInstanceOf(AmazonDocumentDBStorageProvider.class)
                .hasJobMapper();
    }

    @Singleton
    public MongoClient mongoClient() {
        return Mocks.mongoClient();
    }
}