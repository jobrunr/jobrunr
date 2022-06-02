package org.jobrunr.server;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.stubs.SimpleJobActivator;
import org.jobrunr.server.threadpool.JobRunrExecutor;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.stubs.TestServiceForIoC;
import org.jobrunr.utils.mapper.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ServiceLoader;
import java.util.stream.Stream;

import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BackgroundJobServerUsesBackJobPerformerFactoryTest {

    private StorageProvider storageProvider;

    private SimpleJobActivator jobActivator;

    @Mock
    private JobRunrExecutor jobRunrExecutor;
    @Mock
    private ServiceLoader<JobRunrExecutor> jobRunrExecutorServiceLoader;

    @BeforeEach
    void setUp() {
        storageProvider = Mockito.spy(new InMemoryStorageProvider());
        jobActivator = new SimpleJobActivator(new TestServiceForIoC("an argument"));

        when(jobRunrExecutorServiceLoader.iterator()).thenReturn(Stream.of(jobRunrExecutor).iterator());
    }

    @Test
    void testProcessJobUsesBackgroundJobPerformerFactoryWithMaximumPriorityWhenAvailable() {
        try (MockedStatic<ServiceLoader> serviceLoaderMock = Mockito.mockStatic(ServiceLoader.class)) {
            // GIVEN
            BackgroundJobPerformer minPriorityBackgroundJobPerformer = mock(BackgroundJobPerformer.class);
            BackgroundJobPerformerFactory minPriorityJobPerformerFactory = mockBackgroundJobPerformerFactory(minPriorityBackgroundJobPerformer, 5);

            BackgroundJobPerformer maxPriorityBackgroundJobPerformer = mock(BackgroundJobPerformer.class);
            BackgroundJobPerformerFactory maxPriorityJobPerformerFactory = mockBackgroundJobPerformerFactory(maxPriorityBackgroundJobPerformer, 15);

            ServiceLoader<BackgroundJobPerformerFactory> backgroundJobPerformerFactoryServiceLoader = mock(ServiceLoader.class);
            when(backgroundJobPerformerFactoryServiceLoader.iterator()).thenReturn(Stream.of(minPriorityJobPerformerFactory, maxPriorityJobPerformerFactory).iterator());

            serviceLoaderMock.when(() -> ServiceLoader.load(JobRunrExecutor.class)).thenReturn(jobRunrExecutorServiceLoader);
            serviceLoaderMock.when(() -> ServiceLoader.load(BackgroundJobPerformerFactory.class)).thenReturn(backgroundJobPerformerFactoryServiceLoader);

            BackgroundJobServer backgroundJobServer = new BackgroundJobServer(storageProvider, mock(JsonMapper.class), jobActivator, usingStandardBackgroundJobServerConfiguration());
            backgroundJobServer.start();

            // WHEN
            backgroundJobServer.processJob(mock(Job.class));

            // THEN
            verify(minPriorityJobPerformerFactory, never()).newBackgroundJobPerformer(any(), any());
            verify(jobRunrExecutor, never()).execute(eq(minPriorityBackgroundJobPerformer));

            verify(maxPriorityJobPerformerFactory).newBackgroundJobPerformer(any(), any());
            verify(jobRunrExecutor).execute(eq(maxPriorityBackgroundJobPerformer));

            // CLEANUP
            backgroundJobServer.stop();
        }
    }

    @Test
    void testProcessJobUsesDefaultBackgroundJobPerformerFactoryWhenNoneAvailable() {
        try (MockedStatic<ServiceLoader> serviceLoaderMock = Mockito.mockStatic(ServiceLoader.class)) {

            // GIVEN
            ServiceLoader<BackgroundJobPerformerFactory> backgroundJobPerformerFactoryServiceLoader = mock(ServiceLoader.class);
            when(backgroundJobPerformerFactoryServiceLoader.iterator())
                    .thenReturn(Stream.<BackgroundJobPerformerFactory>empty().iterator());

            serviceLoaderMock.when(() -> ServiceLoader.load(JobRunrExecutor.class)).thenReturn(jobRunrExecutorServiceLoader);
            serviceLoaderMock.when(() -> ServiceLoader.load(BackgroundJobPerformerFactory.class)).thenReturn(backgroundJobPerformerFactoryServiceLoader);

            BackgroundJobServer backgroundJobServer = new BackgroundJobServer(storageProvider, mock(JsonMapper.class), jobActivator, usingStandardBackgroundJobServerConfiguration());
            backgroundJobServer.start();

            // WHEN
            backgroundJobServer.processJob(aJobInProgress().build());

            // THEN
            serviceLoaderMock.verify(() -> ServiceLoader.load(BackgroundJobPerformerFactory.class));
            verify(jobRunrExecutor).execute(any(BackgroundJobPerformer.class));

            // CLEANUP
            backgroundJobServer.stop();
        }
    }

    private BackgroundJobPerformerFactory mockBackgroundJobPerformerFactory(BackgroundJobPerformer backgroundJobPerformer, int priority) {
        BackgroundJobPerformerFactory backgroundJobPerformerFactory = mock(BackgroundJobPerformerFactory.class);
        lenient().when(backgroundJobPerformerFactory.newBackgroundJobPerformer(any(), any())).thenReturn(backgroundJobPerformer);
        when(backgroundJobPerformerFactory.getPriority()).thenReturn(priority);
        return backgroundJobPerformerFactory;
    }

}
