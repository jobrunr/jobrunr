package org.jobrunr.storage.sql.oracle;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.executioncondition.RunTestBetween;

import javax.sql.DataSource;

import static org.jobrunr.storage.sql.SqlTestUtils.toHikariDataSource;

@RunTestBetween(from = "00:00", to = "03:00")
class OracleStorageProviderTest extends AbstractOracleStorageProviderTest {

}