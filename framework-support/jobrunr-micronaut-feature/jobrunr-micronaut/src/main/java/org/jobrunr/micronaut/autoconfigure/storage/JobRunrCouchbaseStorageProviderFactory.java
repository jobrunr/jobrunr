package org.jobrunr.micronaut.autoconfigure.storage;

import com.couchbase.client.java.Cluster;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.micronaut.autoconfigure.JobRunrConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.storage.nosql.couchbase.CouchbaseStorageProvider;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

@Factory
@Requires(classes = {Cluster.class})
@Requires(beans = {Cluster.class})
@Requires(property = "jobrunr.database.type", value = "couchbase")
public class JobRunrCouchbaseStorageProviderFactory {

    @Inject
    private JobRunrConfiguration configuration;

    @Singleton
    @Primary
    public StorageProvider couchbaseStorageProvider(Cluster cluster, JobMapper jobMapper) {
        String bucketName = configuration.getDatabase().getDatabaseName().orElse(CouchbaseStorageProvider.DEFAULT_BUCKET_NAME);
        String collectionPrefix = configuration.getDatabase().getTablePrefix().orElse(null);
        StorageProviderUtils.DatabaseOptions databaseOptions = configuration.getDatabase().isSkipCreate() ? StorageProviderUtils.DatabaseOptions.SKIP_CREATE : StorageProviderUtils.DatabaseOptions.CREATE;
        CouchbaseStorageProvider storageProvider = new CouchbaseStorageProvider(
                cluster, bucketName, CouchbaseStorageProvider.DEFAULT_SCOPE_NAME, collectionPrefix, databaseOptions,
                rateLimit().at1Request().per(SECOND));
        storageProvider.setJobMapper(jobMapper);
        return storageProvider;
    }
}
