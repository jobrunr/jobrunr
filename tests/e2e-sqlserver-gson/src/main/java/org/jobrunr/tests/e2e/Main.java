package org.jobrunr.tests.e2e;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

import javax.sql.DataSource;

public class Main extends AbstractSqlMain {

    public static void main(String[] args) throws Exception {
        new Main(args);
    }

    public Main(String[] args) throws Exception {
        super(args);
    }

    @Override
    protected DataSource createDataSource(String jdbcUrl, String userName, String password) {
        SQLServerDataSource dataSource = new SQLServerDataSource();
        dataSource.setURL(jdbcUrl);
        dataSource.setUser(userName);
        dataSource.setPassword(password);
        return dataSource;
    }
}
