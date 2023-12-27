package org.jobrunr.storage.sql.common.db;

import org.jobrunr.utils.annotations.VisibleFor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Arrays.stream;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.storage.sql.common.db.ConcurrentSqlModificationException.concurrentDatabaseModificationException;
import static org.jobrunr.utils.reflection.ReflectionUtils.getValueFromFieldOrProperty;
import static org.jobrunr.utils.reflection.ReflectionUtils.objectContainsFieldOrProperty;

public class Sql<T> {
    private static final String INSERT = "insert ";
    private static final String UPDATE = "update ";
    private static final String DELETE = "delete ";

    private final List<String> paramNames;
    private final Map<String, Object> params;
    private final Map<String, Function<T, ?>> paramSuppliers;

    protected Dialect dialect;
    private String tablePrefix;
    private String suffix = "";

    private static final Map<Integer, ParsedStatement> parsedStatementCache = new ConcurrentHashMap<>();
    private String tableName;
    private Connection connection;

    protected Sql() {
        this.paramNames = new ArrayList<>();
        this.params = new HashMap<>();
        this.paramSuppliers = new HashMap<>();
    }

    public static <T> Sql<T> forType(Class<T> tClass) {
        return new Sql<>();
    }

    public Sql<T> using(Connection connection, Dialect dialect, String tablePrefix, String tableName) {
        this.connection = connection;
        this.dialect = dialect;
        this.tablePrefix = tablePrefix;
        this.tableName = tableName;
        return this;
    }

    public Sql<T> with(String name, Enum<?> value) {
        params.put(name, value.name());
        return this;
    }

    public Sql<T> with(String name, Object value) {
        params.put(name, value);
        return this;
    }

    public Sql<T> with(String name, Function<T, Object> function) {
        paramSuppliers.put(name, function);
        return this;
    }

    public Sql<T> with(String columnName, String sqlName, String value) {
        throw new UnsupportedOperationException("Needs to be implemented in child classes");
    }

    public Sql<T> withVersion(Function<T, Integer> function) {
        paramSuppliers.put("version", function);
        return this;
    }

    public Stream<SqlResultSet> select(String statement) {
        return select(statement, "");
    }

    public Stream<SqlResultSet> select(String statement, String suffix) {
        String parsedStatement = parse("select " + statement + suffix);
        SqlSpliterator sqlSpliterator = new SqlSpliterator(connection, parsedStatement, this::setParams);
        return StreamSupport.stream(sqlSpliterator, false);
    }

    public Stream<SqlResultSet> execute(String statement) {
        String parsedStatement = parse(statement + suffix);
        SqlSpliterator sqlSpliterator = new SqlSpliterator(connection, parsedStatement, this::setParams);
        return StreamSupport.stream(sqlSpliterator, false);
    }

    public long selectCount(String statement) throws SQLException {
        String parsedStatement = parse("select count(*) " + statement);
        try (PreparedStatement ps = connection.prepareStatement(parsedStatement)) {
            setParams(ps);
            try (ResultSet countResultSet = ps.executeQuery()) {
                countResultSet.next();
                return countResultSet.getLong(1);
            }
        }
    }

    public long selectSum(String column) throws SQLException {
        String parsedStatement = parse("select sum(" + column + ") from " + tableName);
        try (PreparedStatement ps = connection.prepareStatement(parsedStatement)) {
            setParams(ps);
            try (ResultSet countResultSet = ps.executeQuery()) {
                countResultSet.next();
                return countResultSet.getLong(1);
            }
        }
    }

    public boolean selectExists(String statement) throws SQLException {
        return selectCount(statement) > 0;
    }

    public void insert(T item, String statement) throws SQLException {
        insertOrUpdate(item, INSERT + statement);
    }

    public void insert(String statement) throws SQLException {
        insertOrUpdate(null, INSERT + statement);
    }

    public void update(String statement) throws SQLException {
        insertOrUpdate(null, UPDATE + statement);
    }

    public void update(T item, String statement) throws SQLException {
        insertOrUpdate(item, UPDATE + statement);
    }

    public int delete(String statement) throws SQLException {
        String parsedStatement = parse(DELETE + statement);
        try (PreparedStatement ps = connection.prepareStatement(parsedStatement)) {
            setParams(ps);
            return ps.executeUpdate();
        }
    }

    private void insertOrUpdate(T item, String statement) throws SQLException {
        String parsedStatement = parse(statement);
        try (PreparedStatement ps = connection.prepareStatement(parsedStatement)) {
            setParams(ps, item);
            final int updated = ps.executeUpdate();
            if (updated != 1) {
                throw concurrentDatabaseModificationException(item, updated);
            }
        } catch (SQLException e) {
            String lowerCaseMessage = e.getMessage().toLowerCase();
            if (e.getErrorCode() == -803 || lowerCaseMessage.contains("duplicate") || lowerCaseMessage.contains("primary key") || lowerCaseMessage.contains("unique constraint")) {
                throw concurrentDatabaseModificationException(item, 0);
            }
            throw e;
        }
    }

    public void insertAll(List<T> batchCollection, String statement) throws SQLException {
        int[] result = insertOrUpdateAll(batchCollection, INSERT + statement);
        if (result.length != batchCollection.size()) {
            throw shouldNotHappenException("Could not insert or update all objects - different result size: originalCollectionSize=" + batchCollection.size() + "; " + Arrays.toString(result));
        } else if (stream(result).anyMatch(i -> i < 1 && i != Statement.SUCCESS_NO_INFO)) {
            throw concurrentDatabaseModificationException(batchCollection, result);
        }
    }

    public void updateAll(List<T> batchCollection, String statement) throws SQLException {
        int[] result = insertOrUpdateAll(batchCollection, UPDATE + statement);
        if (result.length != batchCollection.size()) {
            throw shouldNotHappenException("Could not insert or update all objects - different result size: originalCollectionSize=" + batchCollection.size() + "; " + Arrays.toString(result));
        } else if (stream(result).anyMatch(i -> i < 1 && i != Statement.SUCCESS_NO_INFO)) {
            throw concurrentDatabaseModificationException(batchCollection, result);
        }
    }

    private int[] insertOrUpdateAll(List<T> batchCollection, String statement) throws SQLException {
        String parsedStatement = parse(statement);
        try (PreparedStatement ps = connection.prepareStatement(parsedStatement)) {
            for (T object : batchCollection) {
                setParams(ps, object);
                ps.addBatch();
            }
            return ps.executeBatch();
        }
    }

    private void setParams(PreparedStatement ps) {
        try {
            setParams(ps, null);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setParams(PreparedStatement ps, T object) throws SQLException {
        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = paramNames.get(i);
            if (params.containsKey(paramName)) {
                dialect.setParam(ps, i + 1, params.get(paramName));
            } else if (paramSuppliers.containsKey(paramName)) {
                dialect.setParam(ps, i + 1, paramSuppliers.get(paramName).apply(object));
            } else if (objectContainsFieldOrProperty(object, paramName)) {
                dialect.setParam(ps, i + 1, getValueFromFieldOrProperty(object, paramName));
            } else if ("previousVersion".equals(paramName)) {
                dialect.setParam(ps, i + 1, ((int) paramSuppliers.get("version").apply(object)) - 1);
            } else {
                throw new IllegalArgumentException(String.format("Parameter %s is not known.", paramName));
            }
        }
    }

    final String parse(String query) {
        final ParsedStatement parsedStatement = parsedStatementCache.computeIfAbsent(elementPrefixer(tablePrefix, query).hashCode(), hash -> createParsedStatement(query));
        paramNames.clear();
        paramNames.addAll(parsedStatement.paramNames);
        return parsedStatement.sqlStatement;
    }

    final ParsedStatement createParsedStatement(String query) {
        final String parsedStatement = parseStatement(dialect.escape(query));
        return new ParsedStatement(parsedStatement, new ArrayList<>(paramNames));
    }

    @VisibleFor("testing")
    protected String parseStatement(String query) {
        paramNames.clear();
        // I was originally using regular expressions, but they didn't work well for ignoring
        // parameter-like strings inside quotes.
        int length = query.length();
        StringBuilder parsedQuery = new StringBuilder(length);
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < length; i++) {
            char c = query.charAt(i);
            if (inSingleQuote) {
                if (c == '\'') {
                    inSingleQuote = false;
                }
            } else if (inDoubleQuote) {
                if (c == '"') {
                    inDoubleQuote = false;
                }
            } else {
                if (c == '\'') {
                    inSingleQuote = true;
                } else if (c == '"') {
                    inDoubleQuote = true;
                } else if (c == ':' && i + 1 < length &&
                        Character.isJavaIdentifierStart(query.charAt(i + 1)) && !parsedQuery.toString().endsWith(":")) {
                    int j = i + 2;
                    while (j < length && Character.isJavaIdentifierPart(query.charAt(j))) {
                        j++;
                    }
                    String name = query.substring(i + 1, j);
                    c = '?'; // replace the parameter with a question mark
                    i += name.length(); // skip past the end if the parameter

                    paramNames.add(name);
                }
            }
            parsedQuery.append(c);
        }
        return parsedQuery.toString()
                .replace(tableName, elementPrefixer(tablePrefix, tableName));
    }

    private static class ParsedStatement {
        private final String sqlStatement;
        private final List<String> paramNames;

        public ParsedStatement(String sqlStatement, List<String> paramNames) {
            this.sqlStatement = sqlStatement;
            this.paramNames = paramNames;
        }
    }
}
