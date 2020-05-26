package org.jobrunr.tests.e2e;

import org.jobrunr.storage.SimpleStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;

public class Main extends AbstractMain {

    public static void main(String[] args) throws Exception {
        new Main(args);
    }

    public Main(String[] args) throws Exception {
        super(args);
    }

    @Override
    protected StorageProvider initStorageProvider() {
        return new SimpleStorageProvider()
                .withJsonMapper(new JacksonJsonMapper())
                .withDefaultData()
                .withSomeRecurringJobs();
    }
}
