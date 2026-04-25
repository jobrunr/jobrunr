package org.jobrunr.storage.nosql.couchbase;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.manager.collection.CollectionSpec;
import com.couchbase.client.java.manager.collection.ScopeSpec;
import com.couchbase.client.java.manager.query.CreateQueryIndexOptions;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.StorageProviderUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;

public class CouchbaseCreator {

    private final Cluster cluster;
    private final String bucketName;
    private final String scopeName;
    private final String collectionPrefix;

    private static final List<String> COLLECTION_NAMES = Arrays.asList(
            StorageProviderUtils.Jobs.NAME,
            StorageProviderUtils.RecurringJobs.NAME,
            StorageProviderUtils.BackgroundJobServers.NAME,
            StorageProviderUtils.Metadata.NAME
    );

    public CouchbaseCreator(Cluster cluster, String bucketName, String scopeName, String collectionPrefix) {
        this.cluster = cluster;
        this.bucketName = bucketName;
        this.scopeName = scopeName;
        this.collectionPrefix = collectionPrefix;
    }

    public void runMigrations() {
        createScopeIfNotExists();
        createCollectionsIfNotExist();
        createIndexes();
    }

    public void validateCollections() {
        List<String> existingCollections = getExistingCollectionNames();
        for (String collectionName : COLLECTION_NAMES) {
            String prefixed = elementPrefixer(collectionPrefix, collectionName);
            if (!existingCollections.contains(prefixed)) {
                throw new StorageException("Couchbase collection '" + prefixed + "' in scope '" + scopeName + "' does not exist. Please create it or use DatabaseOptions.CREATE.");
            }
        }
    }

    private void createScopeIfNotExists() {
        try {
            List<String> existingScopes = cluster.bucket(bucketName).collections().getAllScopes()
                    .stream().map(ScopeSpec::name).collect(Collectors.toList());
            if (!existingScopes.contains(scopeName)) {
                cluster.query(String.format("CREATE SCOPE `%s`.`%s`", bucketName, scopeName));
            }
        } catch (Exception e) {
            throw new StorageException("Failed to create Couchbase scope '" + scopeName + "'", e);
        }
    }

    private void createCollectionsIfNotExist() {
        List<String> existingCollections = getExistingCollectionNames();
        for (String collectionName : COLLECTION_NAMES) {
            String prefixed = elementPrefixer(collectionPrefix, collectionName);
            if (!existingCollections.contains(prefixed)) {
                try {
                    cluster.query(String.format("CREATE COLLECTION `%s`.`%s`.`%s`", bucketName, scopeName, prefixed));
                } catch (Exception e) {
                    throw new StorageException("Failed to create Couchbase collection '" + prefixed + "'", e);
                }
            }
        }
    }

    private void createIndexes() {
        createSecondaryIndexes();
    }

    private void createSecondaryIndexes() {
        String jobsCollection = elementPrefixer(collectionPrefix, StorageProviderUtils.Jobs.NAME);
        String bgServersCollection = elementPrefixer(collectionPrefix, StorageProviderUtils.BackgroundJobServers.NAME);
        String metadataCollection = elementPrefixer(collectionPrefix, StorageProviderUtils.Metadata.NAME);
        String recurringJobsCollection = elementPrefixer(collectionPrefix, StorageProviderUtils.RecurringJobs.NAME);

        createIndex(jobsCollection, "idx_jobs_state",
                StorageProviderUtils.Jobs.FIELD_STATE);
        createIndex(jobsCollection, "idx_jobs_state_updatedat",
                StorageProviderUtils.Jobs.FIELD_STATE, StorageProviderUtils.Jobs.FIELD_UPDATED_AT);
        createIndex(jobsCollection, "idx_jobs_state_scheduledat",
                StorageProviderUtils.Jobs.FIELD_STATE, StorageProviderUtils.Jobs.FIELD_SCHEDULED_AT);
        createIndex(jobsCollection, "idx_jobs_recurring_job_id",
                StorageProviderUtils.Jobs.FIELD_RECURRING_JOB_ID, StorageProviderUtils.Jobs.FIELD_SCHEDULED_AT);
        createIndex(bgServersCollection, "idx_bgservers_first_heartbeat",
                StorageProviderUtils.BackgroundJobServers.FIELD_FIRST_HEARTBEAT);
        createIndex(bgServersCollection, "idx_bgservers_last_heartbeat",
                StorageProviderUtils.BackgroundJobServers.FIELD_LAST_HEARTBEAT);
        createIndex(metadataCollection, "idx_metadata_name",
                StorageProviderUtils.Metadata.FIELD_NAME);
        createIndex(recurringJobsCollection, "idx_recurringjobs_createdat",
                StorageProviderUtils.RecurringJobs.FIELD_CREATED_AT);
    }

    private void createIndex(String collectionName, String indexName, String... fields) {
        try {
            List<String> fieldList = Arrays.asList(fields);
            cluster.queryIndexes().createIndex(bucketName, indexName, fieldList,
                    CreateQueryIndexOptions.createQueryIndexOptions()
                            .scopeName(scopeName)
                            .collectionName(collectionName)
                            .ignoreIfExists(true));
        } catch (Exception e) {
            throw new StorageException("Failed to create index '" + indexName + "' on '" + collectionName + "'", e);
        }
    }

    private List<String> getExistingCollectionNames() {
        return cluster.bucket(bucketName).collections().getAllScopes().stream()
                .filter(scope -> scopeName.equals(scope.name()))
                .flatMap(scope -> scope.collections().stream())
                .map(CollectionSpec::name)
                .collect(Collectors.toList());
    }
}
