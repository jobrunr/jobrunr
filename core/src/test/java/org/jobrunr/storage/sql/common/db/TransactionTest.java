package org.jobrunr.storage.sql.common.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionTest {

    @Mock
    private Connection connection;

    @Test
    void testTransactionSucceedsWithAutocommitTrue() throws SQLException {
        //GIVEN
        when(connection.getAutoCommit()).thenReturn(true, false);
        final Transaction transaction = new Transaction(connection);

        //WHEN
        transaction.commit();
        transaction.close();

        //THEN
        verify(connection).commit();
        verify(connection, never()).rollback();
        verify(connection).setAutoCommit(true);
    }

    @Test
    void testTransactionSucceedsWithAutocommitFalse() throws SQLException {
        //GIVEN
        when(connection.getAutoCommit()).thenReturn(false);
        final Transaction transaction = new Transaction(connection);

        //WHEN
        transaction.commit();
        transaction.close();

        //THEN
        verify(connection).commit();
        verify(connection, never()).rollback();
        verify(connection, never()).setAutoCommit(anyBoolean());
    }

    @Test
    void testTransactionFailsWithAutocommitTrue() throws SQLException {
        //GIVEN
        when(connection.getAutoCommit()).thenReturn(true, false);
        final Transaction transaction = new Transaction(connection);

        //WHEN
        transaction.close();

        //THEN
        verify(connection, never()).commit();
        verify(connection).rollback();
        verify(connection).setAutoCommit(true);
    }

    @Test
    void testTransactionFailsWithAutocommitFalse() throws SQLException {
        //GIVEN
        when(connection.getAutoCommit()).thenReturn(false);
        final Transaction transaction = new Transaction(connection);

        //WHEN
        transaction.close();

        //THEN
        verify(connection, never()).commit();
        verify(connection).rollback();
        verify(connection, never()).setAutoCommit(anyBoolean());
    }
}