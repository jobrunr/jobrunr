package org.jobrunr.server.tasks;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProvider.StorageProviderInfo;
import org.jobrunr.storage.sql.postgres.PostgresStorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jobrunr.jobs.JobTestBuilder.aScheduledJob;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MigrateFromV5toV6TaskTest {

    @Mock
    StorageProvider storageProvider;

    @Mock
    BackgroundJobServer backgroundJobServer;

    @Mock
    StorageProviderInfo storageProviderInfo;


    MigrateFromV5toV6Task task;

    @BeforeEach
    void setUpTask() {
        when(backgroundJobServer.getStorageProvider()).thenReturn(storageProvider);
        when(storageProvider.getStorageProviderInfo()).thenReturn(storageProviderInfo);

        task = new MigrateFromV5toV6Task(backgroundJobServer);
    }

    @Test
    void doesNoMigrationsOfScheduledJobsIfStorageProviderIsNotAnSqlStorageProvider() {
        doReturn(InMemoryStorageProvider.class).when(storageProviderInfo).getImplementationClass();

        task.run();

        verify(storageProvider, never()).getScheduledJobs(any(), any());
    }

    @Test
    void doesMigrationsOfScheduledJobsIfStorageProviderIsAnSqlStorageProvider() {
        doReturn(PostgresStorageProvider.class).when(storageProviderInfo).getImplementationClass();

        task.run();

        verify(storageProvider).getScheduledJobs(any(), any());
    }

    @Test
    void doesMigrationsOfAllScheduledJobsIfStorageProviderIsAnSqlStorageProvider() {
        doReturn(PostgresStorageProvider.class).when(storageProviderInfo).getImplementationClass();


        when(storageProvider.getScheduledJobs(any(), any())).thenReturn(
                aLotOfScheduledJobs(5000),
                aLotOfScheduledJobs(500),
                Collections.emptyList()
        );

        task.run();

        verify(storageProvider, times(3)).getScheduledJobs(any(), any());
        verify(storageProvider, times(6)).save(anyList());
    }

    private List<Job> aLotOfScheduledJobs(int amountOfScheduledJobs) {
        ArrayList<Job> jobs = new ArrayList<>();
        for(int i = 0; i < amountOfScheduledJobs; i++) {
            jobs.add(aScheduledJob().build());
        }
        return jobs;
    }

}