package org.jobrunr.tests.e2e;

import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;

public class Main extends AbstractMain {

    public static void main(String[] args) throws Exception {
        new Main(args);
    }

    public Main(String[] args) throws Exception {
        super(args);
    }

    @Override
    protected DataSource createDataSource(String jdbcUrl, String userName, String password) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(jdbcUrl);
        dataSource.setUser(userName);
        dataSource.setPassword(password);
        return dataSource;
    }
}
