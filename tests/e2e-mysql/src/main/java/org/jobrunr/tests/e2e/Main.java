package org.jobrunr.tests.e2e;

import com.mysql.cj.jdbc.MysqlDataSource;

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
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUrl(jdbcUrl + "?rewriteBatchedStatements=true&pool=true");
        dataSource.setUser(userName);
        dataSource.setPassword(password);
        return dataSource;
    }
}
