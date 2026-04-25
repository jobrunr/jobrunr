package org.jobrunr.storage.nosql.couchbase;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryScanConsistency;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderTest;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterAll;
import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;

import java.util.Arrays;

import static org.jobrunr.storage.StorageProviderUtils.elementPrefixer;
import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;

public abstract class AbstractCouchbaseStorageProviderTest extends StorageProviderTest {

    private static Cluster cluster;

    protected abstract CouchbaseContainer getCouchbaseContainer();

    @Override
    protected void cleanup(int testMethodIndex) {
        Cluster cl = cluster();
        String bucketName = CouchbaseStorageProvider.DEFAULT_BUCKET_NAME;
        String scopeName = CouchbaseStorageProvider.DEFAULT_SCOPE_NAME;

        for (String collectionName : Arrays.asList(
                StorageProviderUtils.Jobs.NAME,
                StorageProviderUtils.RecurringJobs.NAME,
                StorageProviderUtils.BackgroundJobServers.NAME,
                StorageProviderUtils.Metadata.NAME)) {
            String prefixed = elementPrefixer(null, collectionName);
            try {
                cl.bucket(bucketName).scope(scopeName).query(
                        "DELETE FROM `" + prefixed + "`",
                        QueryOptions.queryOptions().scanConsistency(QueryScanConsistency.REQUEST_PLUS));
            } catch (Exception ignored) {
                // collection may not exist yet on first run
            }
        }
    }

    @Override
    protected StorageProvider getStorageProvider() {
        CouchbaseStorageProvider storageProvider = new CouchbaseStorageProvider(
                cluster(),
                CouchbaseStorageProvider.DEFAULT_BUCKET_NAME,
                CouchbaseStorageProvider.DEFAULT_SCOPE_NAME,
                null,
                StorageProviderUtils.DatabaseOptions.CREATE,
                rateLimit().withoutLimits());
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        return storageProvider;
    }

    @AfterAll
    public static void closeCluster() {
        if (cluster != null) {
            cluster.disconnect();
            cluster = null;
        }
    }

    protected Cluster cluster() {
        CouchbaseContainer container = getCouchbaseContainer();
        if (cluster == null) {
            cluster = Cluster.connect(
                    container.getConnectionString(),
                    ClusterOptions.clusterOptions(container.getUsername(), container.getPassword()));
        }
        return cluster;
    }

    protected static class ThrowingCouchbaseStorageProvider extends ThrowingStorageProvider {

        public ThrowingCouchbaseStorageProvider(StorageProvider storageProvider) {
            super(storageProvider, "jobCollection");
        }

        @Override
        protected void makeStorageProviderThrowException(StorageProvider storageProvider) {
            com.couchbase.client.java.Collection mockedJobCollection =
                    org.mockito.Mockito.mock(com.couchbase.client.java.Collection.class);
            com.couchbase.client.core.error.CouchbaseException couchbaseException =
                    org.mockito.Mockito.mock(com.couchbase.client.core.error.CouchbaseException.class);
            org.mockito.Mockito.lenient()
                    .when(mockedJobCollection.replace(
                            org.mockito.ArgumentMatchers.anyString(),
                            org.mockito.ArgumentMatchers.any(JsonObject.class),
                            org.mockito.ArgumentMatchers.any(com.couchbase.client.java.kv.ReplaceOptions.class)))
                    .thenThrow(couchbaseException);
            try {
                java.lang.reflect.Field field = storageProvider.getClass().getDeclaredField("jobCollection");
                field.setAccessible(true);
                field.set(storageProvider, mockedJobCollection);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
