package org.assertj.db;

import java.util.HashMap;
import java.util.Map;

public class DatabaseQueriesByProvider {

    private static Map<String, DatabaseQueries> databaseQueryMap = new HashMap<>();

    static {
        databaseQueryMap.put("DB2", new DB2DatabaseQueries());
        databaseQueryMap.put("Microsoft SQL Server", new SQLServerDatabaseQueries());
        databaseQueryMap.put("PostgreSQL", new PostgresDatabaseQueries());
        databaseQueryMap.put("H2", new H2DatabaseQueries());
        databaseQueryMap.put("HSQL Database Engine", new H2DatabaseQueries());
        databaseQueryMap.put("Oracle", new OracleDatabaseQueries());
    }

    public static DatabaseQueries getFor(String provider) {
        return databaseQueryMap.getOrDefault(provider, new DB2DatabaseQueries());
    }

}
