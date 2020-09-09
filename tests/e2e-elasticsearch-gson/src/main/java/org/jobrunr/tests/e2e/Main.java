package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.elasticsearch.ElasticSearchStorageProvider;

public class Main extends AbstractMain {

    public static void main(String[] args) throws Exception {
        new Main(args);
    }

    public Main(String[] args) throws Exception {
        super(args);
    }

    @Override
    protected StorageProvider initStorageProvider() {
        if (getEnvOrProperty("ELASTICSEARCH_HOST") == null) {
            throw new IllegalStateException("Cannot start BackgroundJobServer: environment variable ELASTICSEARCH_HOST is not set");
        }
        if (getEnvOrProperty("ELASTICSEARCH_PORT") == null) {
            throw new IllegalStateException("Cannot start BackgroundJobServer: environment variable ELASTICSEARCH_PORT is not set");
        }

        return new ElasticSearchStorageProvider(getEnvOrProperty("ELASTICSEARCH_HOST"), Integer.parseInt(getEnvOrProperty("ELASTICSEARCH_PORT")));
    }
}
