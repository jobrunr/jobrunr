package org.jobrunr.storage.sql.oracle;

import oracle.jdbc.pool.OracleDataSource;
import org.assertj.core.api.Condition;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.DatabaseCleaner;
import org.jobrunr.storage.sql.common.DefaultSqlStorageProvider;
import org.jobrunr.storage.sql.common.SqlStorageProviderFactory;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.mockito.internal.util.reflection.Whitebox;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@RunTestBetween(from = "00:00", to = "03:00")
//@RunTestIfDockerImageExists("container-registry.oracle.com/database/standard:12.1.0.2")
public class OracleTablePrefixStorageProviderTest extends AbstractOracleStorageProviderTest {

    private static OracleDataSource dataSource;

    @BeforeAll
    void runInitScript() {
        String packageName = OracleTablePrefixStorageProviderTest.class.getPackageName();
        String initScriptPath = packageName.replaceAll("\\.", "/") + "/init_with_schema.sql";
        JdbcDatabaseDelegate containerDelegate = new JdbcDatabaseDelegate(sqlContainer, "");
        ScriptUtils.runInitScript(containerDelegate, initScriptPath);
    }

    @Override
    protected StorageProvider getStorageProvider() {
        final StorageProvider storageProvider = SqlStorageProviderFactory.using(getDataSource(), "SOME_SCHEMA.SOME_PREFIX_", DefaultSqlStorageProvider.DatabaseOptions.CREATE);
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        Whitebox.setInternalState(storageProvider, "changeListenerNotificationRateLimit", rateLimit().withoutLimits());
        return storageProvider;
    }

    @Override
    protected DatabaseCleaner getDatabaseCleaner(DataSource dataSource) {
        return new DatabaseCleaner(dataSource, "SOME_SCHEMA.SOME_PREFIX_");
    }

    @Override
    protected DataSource getDataSource() {
        try {
            if (dataSource == null) {
//                dataSource = new OracleDataSource();
//                dataSource.setURL("jdbc:oracle:thin:@localhost:1527:xe".replace(":xe", ":ORCL"));
//                dataSource.setUser("system");
//                dataSource.setPassword("oracle");

                System.out.println("==========================================================================================");
                System.out.println(sqlContainer.getLogs());
                System.out.println("==========================================================================================");

                dataSource = new OracleDataSource();

                dataSource.setURL(sqlContainer.getJdbcUrl().replace(":xe", ":ORCL"));
                dataSource.setUser(sqlContainer.getUsername());
                dataSource.setPassword(sqlContainer.getPassword());
                dataSource.setServiceName("ORCL");
            }

            return dataSource;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void checkTablesCreatedWithCorrectPrefix() throws SQLException {
        assertThat(dataSource)
                .hasTable("SOME_SCHEMA", "SOME_PREFIX_JOBRUNR_MIGRATIONS")
                .hasTable("SOME_SCHEMA", "SOME_PREFIX_JOBRUNR_RECURRING_JOBS")
                .hasTable("SOME_SCHEMA", "SOME_PREFIX_JOBRUNR_BACKGROUNDJOBSERVERS")
                .hasTable("SOME_SCHEMA", "SOME_PREFIX_JOBRUNR_METADATA")
                .hasIndexesMatching(8, new Condition<>(name -> name.startsWith("SOME_PREFIX_JOBRUNR_"), "Index matches"));
    }
}