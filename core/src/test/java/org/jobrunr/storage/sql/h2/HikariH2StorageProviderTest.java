package org.jobrunr.storage.sql.h2;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;

import java.sql.SQLException;

import static org.jobrunr.storage.sql.SqlTestUtils.toHikariDataSource;

public class HikariH2StorageProviderTest extends AbstractH2StorageProviderTest {

    private static HikariDataSource dataSource;

    @Override
    public HikariDataSource getDataSource() {
        return getDataSource(true);
    }

    protected HikariDataSource getDataSource(boolean autoCommit) {
        if (dataSource == null) {
            dataSource = toHikariDataSource("jdbc:h2:mem:test-hikari;DB_CLOSE_DELAY=-1", "sa", "sa", autoCommit);
        }
        return dataSource;
    }

    @AfterAll
    public static void destroyDatasource() throws SQLException {
        shutdownDatabase(dataSource);
        dataSource.close();
        dataSource = null;
    }
}
