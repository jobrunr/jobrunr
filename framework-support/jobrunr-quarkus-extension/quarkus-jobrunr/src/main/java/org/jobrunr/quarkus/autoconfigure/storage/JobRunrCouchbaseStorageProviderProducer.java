package org.jobrunr.quarkus.autoconfigure.storage;

import com.couchbase.client.java.Cluster;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.nosql.couchbase.CouchbaseStorageProvider;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

public class JobRunrCouchbaseStorageProviderProducer {

    @Produces
    @DefaultBean
    @Singleton
    public StorageProvider storageProvider(Cluster cluster, JobMapper jobMapper, JobRunrRuntimeConfiguration configuration) {
        if (configuration.database().type().isPresent() && !configuration.database().type().get().equalsIgnoreCase("couchbase")) return null;

        String bucketName = configuration.database().databaseName().orElse(CouchbaseStorageProvider.DEFAULT_BUCKET_NAME);
        String collectionPrefix = configuration.database().tablePrefix().orElse(null);
        DatabaseOptions databaseOptions = configuration.database().skipCreate() ? DatabaseOptions.SKIP_CREATE : DatabaseOptions.CREATE;
        CouchbaseStorageProvider storageProvider = new CouchbaseStorageProvider(
                cluster, bucketName, CouchbaseStorageProvider.DEFAULT_SCOPE_NAME, collectionPrefix, databaseOptions,
                rateLimit().at1Request().per(SECOND));
        storageProvider.setJobMapper(jobMapper);
        return storageProvider;
    }
}
