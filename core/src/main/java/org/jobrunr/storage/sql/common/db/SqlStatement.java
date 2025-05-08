package org.jobrunr.storage.sql.common.db;

import org.jobrunr.utils.annotations.VisibleFor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;

public class SqlStatement {

    private final String parsedSql;
    private final String originalSql;
    private final List<String> paramNames;
    private final String hashKey;

    public SqlStatement(String tablePrefix, String tableName, Dialect dialect, String originalSql) {
        this.originalSql = originalSql;
        this.paramNames = new ArrayList<>();
        this.parsedSql = parseStatement(tablePrefix, tableName, dialect.escape(originalSql));
        this.hashKey = elementPrefixer(tablePrefix, originalSql);
    }

    public static int statementKey(String tablePrefix, String originalSql) {
        return Objects.hash(elementPrefixer(tablePrefix, originalSql));
    }

    public String getOriginalSql() {
        return originalSql;
    }

    public String getParsedSql() {
        return parsedSql;
    }

    public List<String> getParamNames() {
        return paramNames;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        SqlStatement that = (SqlStatement) object;
        return Objects.equals(hashKey, that.hashKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hashKey);
    }

    @VisibleFor("testing")
    protected String parseStatement(String tablePrefix, String tableName, String sql) {
        // I was originally using regular expressions, but they didn't work well for ignoring
        // parameter-like strings inside quotes.
        int length = sql.length();
        StringBuilder parsedQuery = new StringBuilder(length);
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < length; i++) {
            char c = sql.charAt(i);
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
                        Character.isJavaIdentifierStart(sql.charAt(i + 1)) && !parsedQuery.toString().endsWith(":")) {
                    int j = i + 2;
                    while (j < length && (Character.isJavaIdentifierPart(sql.charAt(j)) || '-' == sql.charAt(j))) {
                        j++;
                    }
                    String name = sql.substring(i + 1, j);
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
}