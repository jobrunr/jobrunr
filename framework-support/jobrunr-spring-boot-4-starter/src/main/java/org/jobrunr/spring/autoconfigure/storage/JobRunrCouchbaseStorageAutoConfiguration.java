package org.jobrunr.spring.autoconfigure.storage;

import com.couchbase.client.java.Cluster;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.spring.autoconfigure.JobRunrAutoConfiguration;
import org.jobrunr.spring.autoconfigure.JobRunrProperties;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils.DatabaseOptions;
import org.jobrunr.storage.nosql.couchbase.CouchbaseStorageProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.couchbase.autoconfigure.CouchbaseAutoConfiguration;
import org.springframework.context.annotation.Bean;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

@AutoConfiguration(after = CouchbaseAutoConfiguration.class, before = JobRunrAutoConfiguration.class)
@ConditionalOnBean(Cluster.class)
@ConditionalOnProperty(prefix = "jobrunr.database", name = "type", havingValue = "couchbase")
public class JobRunrCouchbaseStorageAutoConfiguration {

    @Bean(name = "storageProvider", destroyMethod = "close")
    @ConditionalOnMissingBean
    public StorageProvider couchbaseStorageProvider(Cluster cluster, JobMapper jobMapper, JobRunrProperties properties) {
        String bucketName = properties.getDatabase().getDatabaseName() != null
                ? properties.getDatabase().getDatabaseName()
                : CouchbaseStorageProvider.DEFAULT_BUCKET_NAME;
        String collectionPrefix = properties.getDatabase().getTablePrefix();
        DatabaseOptions databaseOptions = properties.getDatabase().isSkipCreate() ? DatabaseOptions.SKIP_CREATE : DatabaseOptions.CREATE;
        CouchbaseStorageProvider storageProvider = new CouchbaseStorageProvider(
                cluster, bucketName, CouchbaseStorageProvider.DEFAULT_SCOPE_NAME, collectionPrefix, databaseOptions,
                rateLimit().at1Request().per(SECOND));
        storageProvider.setJobMapper(jobMapper);
        return storageProvider;
    }
}
