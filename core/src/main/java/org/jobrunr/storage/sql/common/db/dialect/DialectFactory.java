package org.jobrunr.storage.sql.common.db.dialect;

import org.jobrunr.JobRunrException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DialectFactory {

    private DialectFactory() {
    }

    private static final OracleDialect oracleDialect = new OracleDialect();
    private static final AnsiDialect ansiDialect = new AnsiDialect();

    public static Dialect forDataSource(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            final String url = connection.getMetaData().getURL();
            if (url.contains("oracle")) {
                return oracleDialect;
            }
            return ansiDialect;
        } catch (SQLException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

}
