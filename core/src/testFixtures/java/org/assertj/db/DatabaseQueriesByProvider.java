package org.assertj.db;

import java.util.HashMap;
import java.util.Map;

public class DatabaseQueriesByProvider {

    private static Map<String, DatabaseQueries> databaseQueryMap = new HashMap<>();

    static {
        databaseQueryMap.put("DB2", new DB2DatabaseQueries());
        databaseQueryMap.put("H2", new H2DatabaseQueries());
        databaseQueryMap.put("HSQL Database Engine", new H2DatabaseQueries());
        databaseQueryMap.put("MariaDB", new MariaDBDatabaseQueries());
        databaseQueryMap.put("MySQL", new MariaDBDatabaseQueries());
        databaseQueryMap.put("Oracle", new OracleDatabaseQueries());
        databaseQueryMap.put("PostgreSQL", new PostgresDatabaseQueries());
        databaseQueryMap.put("Microsoft SQL Server", new SQLServerDatabaseQueries());
    }

    public static DatabaseQueries getFor(String provider) {
        return databaseQueryMap.getOrDefault(provider, new DB2DatabaseQueries());
    }

}
