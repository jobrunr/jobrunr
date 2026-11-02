package org.jobrunr.server.tasks.startup;

import org.jobrunr.JobRunrAssertions;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveCurrentVersionTaskTest {

    StorageProvider storageProvider;

    @Mock
    BackgroundJobServer backgroundJobServer;

    SaveCurrentVersionTask task;

    @BeforeEach
    void setUpTask() {
        storageProvider = new InMemoryStorageProvider();
        when(backgroundJobServer.getStorageProvider()).thenReturn(storageProvider);

        task = new SaveCurrentVersionTask(backgroundJobServer);
    }

    @Test
    void taskSavesCurrentVersionInDatabase() {
        task.run();

        JobRunrAssertions.assertThat(storageProvider.getMetadata("database_version", "cluster")).hasValue(JobRunr.VERSION.toString());
    }

}