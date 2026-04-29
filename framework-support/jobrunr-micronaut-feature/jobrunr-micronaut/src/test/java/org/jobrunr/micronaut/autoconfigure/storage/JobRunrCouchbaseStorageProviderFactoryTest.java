package org.jobrunr.micronaut.autoconfigure.storage;

import com.couchbase.client.java.Cluster;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.couchbase.CouchbaseStorageProvider;
import org.jobrunr.stubs.Mocks;
import org.junit.jupiter.api.Test;

import static org.jobrunr.micronaut.MicronautAssertions.assertThat;

@MicronautTest
@Property(name = "jobrunr.database.skip-create", value = "true")
@Property(name = "jobrunr.database.type", value = "couchbase")
class JobRunrCouchbaseStorageProviderFactoryTest {

    @Inject
    ApplicationContext context;

    @Test
    void couchbaseStorageProviderAutoConfigurationTest() {
        assertThat(context).hasSingleBean(StorageProvider.class);
        assertThat(context.getBean(StorageProvider.class))
                .isInstanceOf(CouchbaseStorageProvider.class)
                .hasJobMapper();
        assertThat(context).doesNotHaveBean(InMemoryStorageProvider.class);
    }

    @Singleton
    public Cluster cluster() {
        return Mocks.couchbaseCluster();
    }
}
