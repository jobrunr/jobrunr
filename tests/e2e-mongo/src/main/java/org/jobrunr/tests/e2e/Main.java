package org.jobrunr.tests.e2e;

import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.nosql.mongo.MongoDBStorageProvider;

public class Main extends AbstractMain {

    public static void main(String[] args) throws Exception {
        new Main(args);
    }

    public Main(String[] args) throws Exception {
        super(args);
    }

    @Override
    protected StorageProvider initStorageProvider() {
        if (getEnvOrProperty("MONGO_HOST") == null) {
            throw new IllegalStateException("Cannot start BackgroundJobServer: environment variable MONGO_HOST is not set");
        }
        if (getEnvOrProperty("MONGO_PORT") == null) {
            throw new IllegalStateException("Cannot start BackgroundJobServer: environment variable MONGO_PORT is not set");
        }

        return new MongoDBStorageProvider(getEnvOrProperty("MONGO_HOST"), Integer.parseInt(getEnvOrProperty("MONGO_PORT")));
    }
}
