package org.jobrunr.dashboard;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.storage.SimpleStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;

public class FrontEndDevelopment {

    public static void main(String[] args) throws InterruptedException {
        StorageProvider storageProvider = new SimpleStorageProvider()
                .withJsonMapper(new JacksonJsonMapper())
                .withDefaultData();
        JobRunr
                .configure()
                .useStorageProvider(storageProvider)
                .useDashboard()
                .initialize();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> Thread.currentThread().interrupt()));

        Thread.currentThread().join();
    }
}
