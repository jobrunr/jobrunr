package org.jobrunr.storage.nosql.mongo;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.jobrunr.storage.nosql.common.NoSqlDatabaseCreator;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.jobrunr.storage.nosql.mongo.migrations.MongoMigration;

import java.time.Instant;

import static com.mongodb.client.model.Filters.eq;
import static org.jobrunr.storage.StorageProviderUtils.Migrations;
import static org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider.toMongoId;


public class MongoDBCreator extends NoSqlDatabaseCreator<MongoMigration> {

    private final MongoDatabase jobrunrDatabase;

    private final MongoCollection<Document> migrationCollection;

    public MongoDBCreator(MongoDBStorageProvider mongoDBStorageProvider, MongoClient mongoClient, String dbName) {
        super(mongoDBStorageProvider);
        this.jobrunrDatabase = mongoClient.getDatabase(dbName);
        this.migrationCollection = jobrunrDatabase.getCollection(Migrations.NAME);
    }

    @Override
    protected boolean isNewMigration(NoSqlMigration noSqlMigration) {
        Document migration = migrationCollection.find(eq(toMongoId(Migrations.FIELD_ID), noSqlMigration.getClassName())).first();
        return migration == null;
    }

    @Override
    protected void runMigration(MongoMigration noSqlMigration) {
        noSqlMigration.runMigration(jobrunrDatabase);
    }

    @Override
    protected boolean markMigrationAsDone(NoSqlMigration noSqlMigration) {
        try {
            Document document = new Document();
            document.put(toMongoId(Migrations.FIELD_ID), noSqlMigration.getClassName());
            document.put(Migrations.FIELD_NAME, noSqlMigration.getClassName());
            document.put(Migrations.FIELD_DATE, Instant.now());
            return migrationCollection.insertOne(document).wasAcknowledged();
        } catch (MongoWriteException e) {
            if (e.getError().getCode() == 11000) {
                return true;
            }
            throw e;
        }
    }
}
