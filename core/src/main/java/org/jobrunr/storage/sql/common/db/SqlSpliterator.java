package org.jobrunr.storage.sql.common.db;

import org.jobrunr.storage.StorageException;
import org.jspecify.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class SqlSpliterator implements Spliterator<SqlResultSet>, AutoCloseable {

    private final ThrowingSqlSupplier<PreparedStatement> preparedStatementCreator;
    private PreparedStatement ps;
    private ResultSet rs;
    private List<String> columns;
    private boolean hasMore;

    public SqlSpliterator(ThrowingSqlSupplier<PreparedStatement> preparedStatementCreator) {
        this.preparedStatementCreator = preparedStatementCreator;
    }

    @Override
    public boolean tryAdvance(Consumer<? super SqlResultSet> consumer) {
        try {
            if (ps == null) {
                init();
                hasMore = rs.next();
                if (!hasMore) {
                    close();
                } else {
                    columns = initColumns(rs);
                }
            }
            if (!hasMore) return false;
            consumer.accept(new SqlResultSet(columns, rs));
            hasMore = rs.next();
            if (!hasMore) {
                close();
            }
            return hasMore;
        } catch (SQLException e) {
            close();
            throw new StorageException(e);
        }
    }

    private void init() {
        try {
            ps = this.preparedStatementCreator.get();
            ps.setFetchSize(128);
            rs = ps.executeQuery();
        } catch (SQLException e) {
            close();
            throw new StorageException(e);
        }
    }

    @Override
    public void close() {
        close(rs);
        close(ps);
    }

    private void close(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            //nothing we can do here
        }
    }

    @Override
    public @Nullable Spliterator<SqlResultSet> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics() {
        return 0;
    }

    private static List<String> initColumns(ResultSet resultSet) throws SQLException {
        List<String> result = new ArrayList<>();
        result.add(null); // SQL in Java is 1 based
        final ResultSetMetaData metaData = resultSet.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            result.add(metaData.getColumnLabel(i).toLowerCase());
        }
        return result;
    }

    @FunctionalInterface
    public interface ThrowingSqlSupplier<T> {
        T get() throws SQLException;
    }
}
