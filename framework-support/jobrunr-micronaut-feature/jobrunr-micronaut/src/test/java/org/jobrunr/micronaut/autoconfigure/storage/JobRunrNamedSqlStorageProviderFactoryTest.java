package org.jobrunr.micronaut.autoconfigure.storage;


import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.SqlStorageProvider;
import org.jobrunr.stubs.Mocks;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.jobrunr.micronaut.MicronautAssertions.assertThat;

@MicronautTest
@Property(name = "jobrunr.database.skip-create", value = "true")
@Property(name = "jobrunr.database.datasource", value = "jobrunr")
class JobRunrNamedSqlStorageProviderFactoryTest {

    @Inject
    ApplicationContext context;

    @Test
    void sqlStorageProviderAutoConfigurationTestWithMultipleNamedDataSources() throws SQLException {
        // GIVEN
        context.registerSingleton(dataSource());
        context.registerSingleton(DataSource.class, dataSource(), Qualifiers.byName("jobrunr"));

        // WHEN & THEN
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
    @Named("jobrunr")
    public DataSource jobRunrDataSource() throws SQLException {
        return Mocks.dataSource();
    }

}
