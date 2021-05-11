package org.jobrunr.storage.nosql.elasticsearch;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
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
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.sleep;
import static org.jobrunr.storage.nosql.elasticsearch.migrations.ElasticSearchMigration.createIndex;
import static org.jobrunr.storage.nosql.elasticsearch.migrations.ElasticSearchMigration.indexExists;
import static org.jobrunr.storage.nosql.elasticsearch.migrations.ElasticSearchMigration.waitForHealthyCluster;
import static org.jobrunr.utils.StringUtils.substringBefore;

public class ElasticSearchDBCreator extends NoSqlDatabaseCreator<ElasticSearchMigration> {

    public static final String JOBRUNR_MIGRATIONS_INDEX_NAME = "jobrunr_" + Migrations.NAME;
    private final RestHighLevelClient client;

    public ElasticSearchDBCreator(NoSqlStorageProvider noSqlStorageProvider, RestHighLevelClient client) {
        super(noSqlStorageProvider);
        this.client = client;

        waitForHealthyCluster(client);
        this.createMigrationsIndexIfNotExists();
    }


    private void createMigrationsIndexIfNotExists() {
        if (indexExists(client, JOBRUNR_MIGRATIONS_INDEX_NAME)) return;

        createIndex(client, JOBRUNR_MIGRATIONS_INDEX_NAME);
    }

    @Override
    protected boolean isNewMigration(NoSqlMigration noSqlMigration) {
        return isNewMigration(noSqlMigration, 0);
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

    private boolean isNewMigration(NoSqlMigration noSqlMigration, int retry) {
        sleep(retry * 500L);
        try {
            waitForHealthyCluster(client);
            GetResponse migration = client.get(new GetRequest(JOBRUNR_MIGRATIONS_INDEX_NAME, substringBefore(noSqlMigration.getClassName(), "_")), RequestOptions.DEFAULT);
            return !migration.isExists();
        } catch (ElasticsearchStatusException e) {
            if (retry < 5) {
                return isNewMigration(noSqlMigration, retry + 1);
            }
            throw e;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }
}
