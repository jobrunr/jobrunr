package org.jobrunr.storage.sql.db2;

import org.jobrunr.storage.sql.common.db.AnsiDialect;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DB2Dialect extends AnsiDialect {

    @Override
    public void setNull(PreparedStatement ps, int i, String paramName) throws SQLException {
        ps.setTimestamp(i, null);
    }
}
