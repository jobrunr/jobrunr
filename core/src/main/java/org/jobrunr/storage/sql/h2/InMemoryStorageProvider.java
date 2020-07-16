package org.jobrunr.storage.sql.h2;

import org.h2.jdbcx.JdbcDataSource;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.utils.mapper.JsonMapper;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class InMemoryStorageProvider extends H2StorageProvider {

    private static Connection connection;

    public InMemoryStorageProvider() {
        super(createDataSource());
    }

    public InMemoryStorageProvider withJsonMapper(JsonMapper jsonMapper) {
        super.setJobMapper(new JobMapper(jsonMapper));
        return this;
    }

    private static DataSource createDataSource() {
        try {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:test");
            dataSource.setUser("sa");
            dataSource.setPassword("sa");
            connection = dataSource.getConnection();
            return dataSource;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws Exception {
        super.close();
        connection.close();
    }
}
