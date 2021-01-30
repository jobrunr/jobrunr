package org.jobrunr.storage.nosql.elasticsearch;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.nosql.NoSqlStorageProvider;
import org.jobrunr.storage.nosql.common.NoSqlDatabaseCreator;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.jobrunr.storage.nosql.elasticsearch.migrations.ElasticSearchMigration;

import java.io.IOException;
import java.time.Instant;

import static org.jobrunr.storage.StorageProviderUtils.Migrations;
import static org.jobrunr.storage.nosql.elasticsearch.migrations.ElasticSearchMigration.indexExists;
import static org.jobrunr.utils.StringUtils.substringBefore;

public class ElasticSearchDBCreator extends NoSqlDatabaseCreator<ElasticSearchMigration> {

    public static final String JOBRUNR_MIGRATIONS_INDEX_NAME = "jobrunr_" + Migrations.NAME;
    private RestHighLevelClient client;

    public ElasticSearchDBCreator(NoSqlStorageProvider noSqlStorageProvider, RestHighLevelClient client) {
        super(noSqlStorageProvider);
        this.client = client;
        this.createMigrationsIndexIfNotExists();
    }

    private void createMigrationsIndexIfNotExists() {
        try {
            if (indexExists(client, JOBRUNR_MIGRATIONS_INDEX_NAME)) return;

            client.indices().create(new CreateIndexRequest(JOBRUNR_MIGRATIONS_INDEX_NAME), RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    protected boolean isNewMigration(NoSqlMigration noSqlMigration) {
        try {
            GetResponse migration = client.get(new GetRequest(JOBRUNR_MIGRATIONS_INDEX_NAME, substringBefore(noSqlMigration.getClassName(), "_")), RequestOptions.DEFAULT);
            return !migration.isExists();
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    protected void runMigration(ElasticSearchMigration noSqlMigration) throws IOException {
        noSqlMigration.runMigration(client);
    }

    @Override
    protected boolean markMigrationAsDone(NoSqlMigration noSqlMigration) {
        try {
            XContentBuilder builder = JsonXContent.contentBuilder().prettyPrint();
            builder.startObject();
            builder.field(Migrations.FIELD_NAME, noSqlMigration.getClassName());
            builder.field(Migrations.FIELD_DATE, Instant.now());
            builder.endObject();
            IndexRequest indexRequest = new IndexRequest(JOBRUNR_MIGRATIONS_INDEX_NAME)
                    .id(substringBefore(noSqlMigration.getClassName(), "_"))
                    .setRefreshPolicy(RefreshPolicy.IMMEDIATE)
                    .source(builder);
            return client.index(indexRequest, RequestOptions.DEFAULT) != null;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }
}
