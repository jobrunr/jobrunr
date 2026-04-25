package org.jobrunr.storage.nosql.couchbase;

import org.testcontainers.couchbase.BucketDefinition;
import org.testcontainers.couchbase.CouchbaseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class CouchbaseStorageProviderTest extends AbstractCouchbaseStorageProviderTest {

    @Container
    private static final CouchbaseContainer couchbaseContainer = new CouchbaseContainer("couchbase/server:community-7.6.2")
            .withBucket(new BucketDefinition(CouchbaseStorageProvider.DEFAULT_BUCKET_NAME));

    @Override
    protected CouchbaseContainer getCouchbaseContainer() {
        return couchbaseContainer;
    }

    @Override
    protected ThrowingStorageProvider makeThrowingStorageProvider(org.jobrunr.storage.StorageProvider storageProvider) {
        return new ThrowingCouchbaseStorageProvider(storageProvider);
    }
}
