package org.jobrunr.storage.nosql.mongo;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.jobrunr.JobRunrException;
import org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;
import org.jobrunr.storage.StorageProviderUtils.Jobs;
import org.jobrunr.storage.StorageProviderUtils.Metadata;
import org.jobrunr.storage.StorageProviderUtils.RecurringJobs;
import org.jobrunr.storage.nosql.common.NoSqlDatabaseCreator;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.jobrunr.storage.nosql.mongo.migrations.MongoMigration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static java.util.Arrays.asList;
import static org.jobrunr.storage.StorageProviderUtils.Migrations;
import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider.toMongoId;


public class MongoDBCreator extends NoSqlDatabaseCreator<MongoMigration> {

    private final MongoDatabase jobrunrDatabase;
    private final String collectionPrefix;

    private final MongoCollection<Document> migrationCollection;

    public MongoDBCreator(MongoClient mongoClient, String dbName) {
        this(mongoClient, dbName, null);
    }

    public MongoDBCreator(MongoClient mongoClient, String dbName, String collectionPrefix) {
        super(MongoDBStorageProvider.class);
        this.jobrunrDatabase = mongoClient.getDatabase(dbName);
        this.collectionPrefix = collectionPrefix;
        this.migrationCollection = jobrunrDatabase.getCollection(elementPrefixer(collectionPrefix, Migrations.NAME));
    }

    public void validateCollections() {
        final List<String> requiredCollectionNames = asList(Jobs.NAME, RecurringJobs.NAME, BackgroundJobServers.NAME, Metadata.NAME);
        final List<String> availableCollectionNames = jobrunrDatabase.listCollectionNames().into(new ArrayList<>());
        for (String requiredCollectionName : requiredCollectionNames) {
            if (!availableCollectionNames.contains(elementPrefixer(collectionPrefix, requiredCollectionName))) {
                throw new JobRunrException("Not all required collections are available by JobRunr!");
            }
        }
    }

    @Override
    protected boolean isNewMigration(NoSqlMigration noSqlMigration) {
        Document migration = migrationCollection.find(eq(toMongoId(Migrations.FIELD_ID), noSqlMigration.getClassName())).first();
        return migration == null;
    }

    @Override
    protected void runMigration(MongoMigration noSqlMigration) {
        noSqlMigration.runMigration(jobrunrDatabase, collectionPrefix);
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
