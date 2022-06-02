package org.jobrunr.storage.sql.common;

import org.jobrunr.storage.sql.SqlStorageProvider;
import org.jobrunr.storage.sql.common.migrations.SqlMigration;
import org.jobrunr.storage.sql.db2.DB2StorageProvider;
import org.jobrunr.storage.sql.h2.H2StorageProvider;
import org.jobrunr.storage.sql.mariadb.MariaDbStorageProvider;
import org.jobrunr.storage.sql.oracle.OracleStorageProvider;
import org.jobrunr.storage.sql.postgres.PostgresStorageProvider;
import org.jobrunr.storage.sql.sqlite.SqLiteStorageProvider;
import org.jobrunr.storage.sql.sqlserver.SQLServerStorageProvider;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class DatabaseSqlMigrationFileProvider {

    private static Map<String, Class<? extends SqlStorageProvider>> databaseTypes  = new HashMap<String, Class<? extends SqlStorageProvider>>() {{
        put("db2", DB2StorageProvider.class);
        put("h2", H2StorageProvider.class);
        put("mariadb", MariaDbStorageProvider.class);
        put("mysql", MariaDbStorageProvider.class);
        put("oracle", OracleStorageProvider.class);
        put("postgres", PostgresStorageProvider.class);
        put("sqlite", SqLiteStorageProvider.class);
        put("sqlserver", SQLServerStorageProvider.class);
    }};

    public static void main(String[] args) {
        if(args.length < 1 || !databaseTypes.containsKey(args[0].toLowerCase())) {
            System.out.println("Error: insufficient arguments");
            System.out.println();
            System.out.println("usage: java -cp jobrunr-${jobrunr.version}.jar org.jobrunr.storage.sql.common.DatabaseSqlMigrationFileProvider {databaseType}");
            System.out.println("  where databaseType is one of 'db2', 'h2', 'mariadb', 'mysql', 'oracle', 'postgres', 'sqlite', 'sqlserver'");
            return;
        }

        try {
            System.out.println("==========================================================");
            System.out.println("======== JobRunr Database SQL Migration Provider =========");
            System.out.println("==========================================================");
            DatabaseMigrationsProvider databaseMigrationsProvider = new DatabaseMigrationsProvider(databaseTypes.get(args[0].toLowerCase()));
            databaseMigrationsProvider.getMigrations()
                    .forEach(DatabaseSqlMigrationFileProvider::createSQLMigrationFile);
            System.out.println("Successfully created all SQL scripts for " + args[0] + "!");
        } catch (Exception e) {
            System.out.println("An error occurred: ");
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            System.out.println(exceptionAsString);
        }
    }

    private static void createSQLMigrationFile(SqlMigration migration) {
        try {
            Files.write(Paths.get("./" + migration.getFileName()), migration.getMigrationSql().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
