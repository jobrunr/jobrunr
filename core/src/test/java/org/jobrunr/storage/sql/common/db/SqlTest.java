package org.jobrunr.storage.sql.common.db;


import org.h2.jdbcx.JdbcDataSource;
import org.jobrunr.jobs.Job;
import org.jobrunr.storage.sql.common.db.dialect.Dialect;
import org.jobrunr.storage.sql.common.db.dialect.H2Dialect;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class SqlTest {

    @Test
    void statementsAreCached() throws SQLException {
        DataSource dataSource = getH2DataSource("without-tableprefix");
        Dialect dialect = new H2Dialect();

        try(Connection connection = dataSource.getConnection()) {
            TestSql testSqlWithoutPrefix1 = new TestSql(connection, dialect, null);
            assertThat(testSqlWithoutPrefix1.aCachedSelectStatementThatReturnsTheCount()).isEqualTo(1L);
            assertThat(testSqlWithoutPrefix1.parseStatementCounter).isEqualTo(1L);

            TestSql testSqlWithoutPrefix2 = new TestSql(connection, dialect, null);
            assertThat(testSqlWithoutPrefix2.aCachedSelectStatementThatReturnsTheCount()).isEqualTo(1L);
            assertThat(testSqlWithoutPrefix2.parseStatementCounter).isEqualTo(0L);
        }
    }

    @Test
    void statementsAreCachedButTakeIntoAccountTablePrefix() throws SQLException {
        DataSource dataSource = getH2DataSource("with-tableprefix");
        Dialect dialect = new H2Dialect();

        try(Connection connection = dataSource.getConnection()) {
            TestSql testSqlWithoutPrefix = new TestSql(connection, dialect, null);
            assertThat(testSqlWithoutPrefix.aCachedSelectStatementThatReturnsTheCount()).isEqualTo(1L);

            TestSql testSqlWithPrefix = new TestSql(connection, dialect, "prefix_");
            assertThat(testSqlWithPrefix.aCachedSelectStatementThatReturnsTheCount()).isEqualTo(0L);
        }
    }

    class TestSql extends Sql<Job> {

        int parseStatementCounter = 0;

        public TestSql(Connection connection, Dialect dialect, String tablePrefix) {
            this.using(connection, dialect, tablePrefix, "jobrunr_jobs");
        }

        long aCachedSelectStatementThatReturnsTheCount() throws SQLException {
            return selectCount("from jobrunr_jobs");
        }

        @Override
        String parseStatement(String query) {
            parseStatementCounter++;
            return super.parseStatement(query);
        }
    }

    protected DataSource getH2DataSource(String name) throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:/" + name + ";DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();){
           statement.execute("create table jobrunr_jobs (ID int primary key, jobSignature varchar(50))");
           statement.execute("insert into jobrunr_jobs (ID, jobSignature) values (1, 'Not important')");
           statement.execute("create table prefix_jobrunr_jobs (ID int primary key, jobSignature varchar(50))");
        }

        return dataSource;
    }
}