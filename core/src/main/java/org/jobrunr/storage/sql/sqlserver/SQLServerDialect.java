package org.jobrunr.storage.sql.sqlserver;

import org.jobrunr.storage.sql.common.db.Dialect;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SQLServerDialect implements Dialect {

    @Override
    public String limit() {
        return "OFFSET 0 ROWS FETCH NEXT :limit ROWS ONLY";
    }

    @Override
    public String limitAndOffset() {
        return "OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";
    }

    @Override
    public String escape(String toEscape) {
        if (toEscape.endsWith(selectForUpdateSkipLocked())) {
            return toEscape.substring(0, toEscape.length() - selectForUpdateSkipLocked().length())
                    .replaceFirst(" where ", " with(UPDLOCK, ROWLOCK, READPAST) where ");
        }
        return toEscape;
    }

    @Override
    public String selectForUpdateSkipLocked() {
        return " SELECTFORUPDATE";
    }

    @Override
    public void setNull(PreparedStatement ps, int i, String paramName) throws SQLException {
        ps.setObject(i, null);
    }
}
