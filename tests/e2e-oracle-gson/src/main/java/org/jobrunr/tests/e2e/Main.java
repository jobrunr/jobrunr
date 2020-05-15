package org.jobrunr.tests.e2e;

import oracle.jdbc.pool.OracleDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;

public class Main extends AbstractSqlMain {

    public static void main(String[] args) throws Exception {
        new Main(args);
    }

    public Main(String[] args) throws Exception {
        super(args);
    }

    @Override
    protected DataSource createDataSource(String jdbcUrl, String userName, String password) throws SQLException {
        OracleDataSource dataSource = new OracleDataSource();
        dataSource.setURL(jdbcUrl.replace(":xe", ":ORCL"));
        dataSource.setUser(userName);
        dataSource.setPassword(password);
        dataSource.setServiceName("ORCL");
        return dataSource;
    }
}
