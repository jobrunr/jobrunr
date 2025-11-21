package org.jobrunr.storage.sql.common.db;

import org.jobrunr.utils.annotations.VisibleFor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import static org.jobrunr.storage.sql.common.db.ConcurrentSqlModificationException.concurrentDatabaseModificationException;
import static org.jobrunr.utils.reflection.ReflectionUtils.getValueFromFieldOrProperty;
import static org.jobrunr.utils.reflection.ReflectionUtils.objectContainsFieldOrProperty;

public class Sql<T> {
    private static final String INSERT = "insert ";
    private static final String UPDATE = "update ";
    private static final String DELETE = "delete ";

    private final Map<String, Object> params;
    private final Map<String, Function<T, ?>> paramSuppliers;

    protected Dialect dialect;
    private String tablePrefix;

    private static final Map<Integer, SqlStatement> parsedStatementCache = new ConcurrentHashMap<>();
    private String tableName;
    private Connection connection;

    protected Sql() {
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

    public Sql<T> with(String name, Object value) {
        params.put(name, value);
        return this;
    }

    public Sql<T> with(String name, Function<T, Object> function) {
        paramSuppliers.put(name, function);
        return this;
    }

    public Sql<T> withVersion(Function<T, Integer> function) {
        paramSuppliers.put("version", function);
        return this;
    }

    public Stream<SqlResultSet> select(String statement) {
        return select(statement, "");
    }

    public Stream<SqlResultSet> select(String statement, String suffix) {
        SqlSpliterator sqlSpliterator = new SqlSpliterator(() -> prepareStatementWithParams("select " + statement + suffix));
        return StreamSupport.stream(sqlSpliterator, false);
    }

    public long selectCount(String statement) throws SQLException {
        try (PreparedStatement ps = prepareStatementWithParams("select count(*) " + statement); ResultSet countResultSet = ps.executeQuery()) {
            countResultSet.next();
            return countResultSet.getLong(1);
        }
    }

    public long selectSum(String column) throws SQLException {
        try (PreparedStatement ps = prepareStatementWithParams("select sum(" + column + ") from " + tableName); ResultSet sumResultSet = ps.executeQuery()) {
            sumResultSet.next();
            return sumResultSet.getLong(1);
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
        try (PreparedStatement ps = prepareStatementWithParams(DELETE + statement)) {
            return ps.executeUpdate();
        }
    }

    protected void insertOrUpdate(T item, String statement) throws SQLException {
        insertOrUpdate(item, statement, updated -> updated >= 1);
    }

    private void insertOrUpdate(T item, String statement, Function<Integer, Boolean> expectedUpdatedRows) throws SQLException {
        SqlStatement sqlStatement = parse(statement);
        try (PreparedStatement ps = prepareStatement(sqlStatement)) {
            setParams(sqlStatement, ps, item);
            final int updated = ps.executeUpdate();
            if (!expectedUpdatedRows.apply(updated)) {
                throw concurrentDatabaseModificationException(item, updated);
            }
        }
    }

    public void insertAll(List<T> batchCollection, String statement) throws SQLException {
        int[] result = insertOrUpdateAll(batchCollection, INSERT + statement);
        if (result.length != batchCollection.size()) {
            throw shouldNotHappenException("Could not insert all objects - different result size: originalCollectionSize=" + batchCollection.size() + "; " + Arrays.toString(result));
        } else if (stream(result).anyMatch(i -> i < 1 && i != Statement.SUCCESS_NO_INFO)) {
            throw concurrentDatabaseModificationException(batchCollection, result);
        }
    }

    public void updateAll(List<T> batchCollection, String statement) throws SQLException {
        int[] result = insertOrUpdateAll(batchCollection, UPDATE + statement);
        if (result.length != batchCollection.size()) {
            throw shouldNotHappenException("Could not update all objects - different result size: originalCollectionSize=" + batchCollection.size() + "; " + Arrays.toString(result));
        } else if (stream(result).anyMatch(i -> i < 1 && i != Statement.SUCCESS_NO_INFO)) {
            throw concurrentDatabaseModificationException(batchCollection, result);
        }
    }

    private int[] insertOrUpdateAll(List<T> batchCollection, String statement) throws SQLException {
        if (batchCollection.isEmpty()) return new int[0];

        SqlStatement sqlStatement = parse(statement);
        try (PreparedStatement ps = prepareStatement(sqlStatement)) {
            for (T object : batchCollection) {
                setParams(sqlStatement, ps, object);
                ps.addBatch();
            }
            return ps.executeBatch();
        }
    }

    private PreparedStatement prepareStatementWithParams(String statement) throws SQLException {
        SqlStatement sqlStatement = parse(statement);
        PreparedStatement preparedStatement = prepareStatement(sqlStatement);
        setParams(sqlStatement, preparedStatement, null);
        return preparedStatement;
    }

    private PreparedStatement prepareStatement(SqlStatement sqlStatement) throws SQLException {
        return connection.prepareStatement(sqlStatement.getParsedSql(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    }

    private void setParams(SqlStatement sqlStatement, PreparedStatement ps, T object) throws SQLException {
        for (int i = 0; i < sqlStatement.getParamNames().size(); i++) {
            String paramName = sqlStatement.getParamNames().get(i);
            Object paramValue = getParamValue(paramName, object);
            dialect.setParam(ps, i + 1, paramName, paramValue);
        }
        params.clear();
    }

    final SqlStatement parse(String originalSql) {
        return parsedStatementCache.computeIfAbsent(
                SqlStatement.statementKey(tablePrefix, originalSql),
                hash -> parseStatement(originalSql));
    }

    @VisibleFor("testing")
    protected SqlStatement parseStatement(String originalSql) {
        return new SqlStatement(tablePrefix, tableName, dialect, originalSql);
    }

    private Object getParamValue(String paramName, T object) {
        if (params.containsKey(paramName)) {
            return params.get(paramName);
        } else if (paramSuppliers.containsKey(paramName)) {
            return paramSuppliers.get(paramName).apply(object);
        } else if (objectContainsFieldOrProperty(object, paramName)) {
            return getValueFromFieldOrProperty(object, paramName);
        } else if ("previousVersion".equals(paramName)) {
            return ((int) paramSuppliers.get("version").apply(object)) - 1;
        } else if (paramName.contains("-") && params.containsKey(paramName.split("-", 0)[0])) {
            String[] splitParam = paramName.split("-", 0);
            return ((List<?>) params.get(splitParam[0])).get(Integer.parseInt(splitParam[1]));
        } else {
            throw new IllegalArgumentException(String.format("Parameter %s is not known.", paramName));
        }
    }
}
