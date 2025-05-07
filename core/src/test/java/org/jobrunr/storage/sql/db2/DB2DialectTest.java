package org.jobrunr.storage.sql.db2;

import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DB2DialectTest {

    // why: DB2 does not support setNull
    @Test
    void testSetNull() throws SQLException {
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        DB2Dialect db2Dialect = new DB2Dialect();
        db2Dialect.setNull(preparedStatement, 1, "state");

        verify(preparedStatement).setTimestamp(1, null);
    }
}