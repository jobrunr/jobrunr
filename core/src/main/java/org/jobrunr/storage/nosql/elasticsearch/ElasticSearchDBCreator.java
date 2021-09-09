package org.jobrunr.storage.nosql.elasticsearch;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.jobrunr.JobRunrException;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.StorageProviderUtils.BackgroundJobServers;
import org.jobrunr.storage.StorageProviderUtils.Jobs;
import org.jobrunr.storage.StorageProviderUtils.Metadata;
import org.jobrunr.storage.StorageProviderUtils.RecurringJobs;
import org.jobrunr.storage.nosql.NoSqlStorageProvider;
import org.jobrunr.storage.nosql.common.NoSqlDatabaseCreator;
import org.jobrunr.storage.nosql.common.migrations.NoSqlMigration;
import org.jobrunr.storage.nosql.elasticsearch.migrations.ElasticSearchMigration;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.elasticsearch.rest.RestStatus.NOT_FOUND;
import static org.jobrunr.storage.StorageProviderUtils.Migrations;
import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.JOBRUNR_PREFIX;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.sleep;
import static org.jobrunr.storage.nosql.elasticsearch.migrations.ElasticSearchMigration.createIndex;
import static org.jobrunr.storage.nosql.elasticsearch.migrations.ElasticSearchMigration.indexExists;
import static org.jobrunr.storage.nosql.elasticsearch.migrations.ElasticSearchMigration.waitForHealthyCluster;
import static org.jobrunr.utils.CollectionUtils.asSet;
import static org.jobrunr.utils.StringUtils.substringBefore;

public class ElasticSearchDBCreator extends NoSqlDatabaseCreator<ElasticSearchMigration> {

    public static final String JOBRUNR_MIGRATIONS_INDEX_NAME = JOBRUNR_PREFIX + Migrations.NAME;
    private final RestHighLevelClient client;
    private final String indexPrefix;
    private final String migrationIndexName;

    public ElasticSearchDBCreator(NoSqlStorageProvider noSqlStorageProvider, RestHighLevelClient client, String indexPrefix) {
        super(noSqlStorageProvider);
        this.client = client;
        this.indexPrefix = indexPrefix;
        this.migrationIndexName = elementPrefixer(indexPrefix, JOBRUNR_MIGRATIONS_INDEX_NAME);

        waitForHealthyCluster(client);
    }

    @Override
    public void runMigrations() {
        createMigrationsIndexIfNotExists();
        super.runMigrations();
    }

    public void validateIndices() {
        try {
            waitForHealthyCluster(client);

            final List<String> requiredIndexNames = asList(Jobs.NAME, RecurringJobs.NAME, BackgroundJobServers.NAME, Metadata.NAME);
            final Set<String> availableIndexNames = asSet(client.indices().get(new GetIndexRequest("*"), RequestOptions.DEFAULT).getIndices());
            for (String requiredIndexName : requiredIndexNames) {
                if (!availableIndexNames.contains(elementPrefixer(indexPrefix, elementPrefixer(JOBRUNR_PREFIX, requiredIndexName)))) {
                    throw new JobRunrException("Not all required indices are available by JobRunr!");
                }
            }
        } catch (ElasticsearchStatusException e) {
            if (e.status() == NOT_FOUND) {
                throw new JobRunrException("Not all required indices are available by JobRunr!");
            }
            throw new StorageException(e);
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    private void createMigrationsIndexIfNotExists() {
        if (indexExists(client, migrationIndexName)) return;

        createIndex(client, migrationIndexName);
    }

    @Override
    protected boolean isNewMigration(NoSqlMigration noSqlMigration) {
        return isNewMigration(noSqlMigration, 0);
    }

    @Override
    protected void runMigration(ElasticSearchMigration noSqlMigration) throws IOException {
        noSqlMigration.runMigration(client, indexPrefix);
    }

    @Override
    protected boolean markMigrationAsDone(NoSqlMigration noSqlMigration) {
        try {
            XContentBuilder builder = JsonXContent.contentBuilder().prettyPrint();
            builder.startObject();
            builder.field(Migrations.FIELD_NAME, noSqlMigration.getClassName());
            builder.field(Migrations.FIELD_DATE, Instant.now());
            builder.endObject();
            IndexRequest indexRequest = new IndexRequest(migrationIndexName)
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
            GetResponse migration = client.get(new GetRequest(migrationIndexName, substringBefore(noSqlMigration.getClassName(), "_")), RequestOptions.DEFAULT);
            return !migration.isExists();
        } catch (ElasticsearchStatusException e) {
            if (retry < 5) {
                return isNewMigration(noSqlMigration, retry + 1);
            }
            if (e.status() == NOT_FOUND) {
                return true;
            }
            throw e;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }
}
