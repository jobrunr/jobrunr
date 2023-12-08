package org.jobrunr.micronaut.autoconfigure.storage;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.SqlStorageProvider;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

import static org.jobrunr.micronaut.MicronautAssertions.assertThat;

@MicronautTest
@Property(name = "jobrunr.database.skip-create", value = "true")
@Property(name = "jobrunr.database.type", value = "sql")
class JobRunrMultipleStorageProviderFactoryTest {

    @Inject
    ApplicationContext context;

    @Test
    void sqlStorageProviderAutoConfigurationTest() {
        assertThat(context).hasSingleBean(StorageProvider.class);
        assertThat(context.getBean(StorageProvider.class))
                .isInstanceOf(SqlStorageProvider.class)
                .hasJobMapper();
        assertThat(context).doesNotHaveBean(InMemoryStorageProvider.class);
    }

    @Singleton
    public DataSource dataSource() throws SQLException {
        return Mocks.dataSource();
    }

    @Singleton
    public ElasticsearchClient elasticsearchClient() throws IOException {
        return Mocks.elasticsearchClient();
    }

}
