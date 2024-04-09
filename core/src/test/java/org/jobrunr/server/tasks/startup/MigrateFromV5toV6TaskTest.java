package org.jobrunr.server.tasks.startup;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.Page;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProvider.StorageProviderInfo;
import org.jobrunr.storage.navigation.OffsetBasedPageRequest;
import org.jobrunr.storage.sql.postgres.PostgresStorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.jobrunr.jobs.JobTestBuilder.aScheduledJob;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        when(storageProvider.getScheduledJobs(any(), any()))
                .thenAnswer(i -> aLotOfScheduledJobs(i.getArgument(1, OffsetBasedPageRequest.class), 5500));

        task.run();

        verify(storageProvider, times(2)).getScheduledJobs(any(), any());
        verify(storageProvider, times(6)).save(anyList());
    }

    private Page<Job> aLotOfScheduledJobs(OffsetBasedPageRequest pageRequest, int totalAmountOfScheduledJobs) {
        ArrayList<Job> jobs = new ArrayList<>();
        for (int i = (int) pageRequest.getOffset(); i < (pageRequest.getOffset() == 0 ? pageRequest.getLimit() : totalAmountOfScheduledJobs); i++) {
            jobs.add(aScheduledJob().build());
        }
        return pageRequest.mapToNewPage(totalAmountOfScheduledJobs, jobs);
    }
}