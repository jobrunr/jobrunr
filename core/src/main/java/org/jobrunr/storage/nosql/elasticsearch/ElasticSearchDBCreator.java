package org.jobrunr.storage.nosql.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.transport.endpoints.BooleanResponse;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static co.elastic.clients.elasticsearch._types.Refresh.True;
import static java.util.Arrays.asList;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.jobrunr.storage.StorageProviderUtils.Migrations;
import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.JOBRUNR_PREFIX;
import static org.jobrunr.storage.nosql.elasticsearch.ElasticSearchUtils.sleep;
import static org.jobrunr.storage.nosql.elasticsearch.migrations.ElasticSearchMigration.*;
import static org.jobrunr.utils.StringUtils.substringBefore;

public class ElasticSearchDBCreator extends NoSqlDatabaseCreator<ElasticSearchMigration> {

    public static final String JOBRUNR_MIGRATIONS_INDEX_NAME = JOBRUNR_PREFIX + Migrations.NAME;
    private final ElasticsearchClient client;
    private final String indexPrefix;
    private final String migrationIndexName;

    public ElasticSearchDBCreator(NoSqlStorageProvider noSqlStorageProvider, ElasticsearchClient client, String indexPrefix) {
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
            final Set<String> availableIndexNames = client.indices().get(r -> r.index("*")).result().keySet();
            for (String requiredIndexName : requiredIndexNames) {
                if (!availableIndexNames.contains(elementPrefixer(indexPrefix, elementPrefixer(JOBRUNR_PREFIX, requiredIndexName)))) {
                    throw new JobRunrException("Not all required indices are available by JobRunr!");
                }
            }
        } catch (ElasticsearchException e) {
            if (e.status() == SC_NOT_FOUND) {
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
    protected boolean markMigrationAsDone(final NoSqlMigration noSqlMigration) {
        try {
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put(Migrations.FIELD_NAME, noSqlMigration.getClassName());
            map.put(Migrations.FIELD_DATE, Instant.now().toEpochMilli());
            return client.index(r ->
              r.index(migrationIndexName)
                .id(substringBefore(noSqlMigration.getClassName(), "_"))
                .refresh(True)
                .document(map)
            ).result() != null;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    private boolean isNewMigration(final NoSqlMigration noSqlMigration, final int retry) {
        sleep(retry * 500L);
        try {
            waitForHealthyCluster(client);
            final BooleanResponse migration = client.exists(
              r -> r.index(migrationIndexName).id(substringBefore(noSqlMigration.getClassName(), "_"))
            );
            return !migration.value();
        } catch (ElasticsearchException e) {
            if (retry < 5) {
                return isNewMigration(noSqlMigration, retry + 1);
            }
            if (e.status() == SC_NOT_FOUND) {
                return true;
            }
            throw e;
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }
}
