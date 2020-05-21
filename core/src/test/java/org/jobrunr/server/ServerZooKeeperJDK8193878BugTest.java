package org.jobrunr.server;

import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// see https://bugs.openjdk.java.net/browse/JDK-8193878
@ExtendWith(MockitoExtension.class)
class ServerZooKeeperJDK8193878BugTest {

    @Mock
    private BackgroundJobServer backgroundJobServer;

    @Mock
    private BackgroundJobServerStatus backgroundJobServerStatus;

    @Mock
    private StorageProvider storageProvider;

    @Captor
    private ArgumentCaptor<BackgroundJobServerStatus> backgroundJobServerStatusArgumentCaptor;

    private ServerZooKeeper.BackgroundJobServerStatusWriteModel backgroundJobServerStatusWriteModelSpy;

    private ServerZooKeeper serverZooKeeper;

    @BeforeEach
    void setUp() {
        when(backgroundJobServer.getStorageProvider()).thenReturn(storageProvider);

        backgroundJobServerStatusWriteModelSpy = Mockito.spy(new ServerZooKeeper.BackgroundJobServerStatusWriteModel(backgroundJobServerStatus));
        serverZooKeeper = new ServerZooKeeper(backgroundJobServer) {
            @Override
            protected BackgroundJobServerStatusWriteModel getBackgroundJobServerStatusWriteModel(BackgroundJobServer backgroundJobServer) {
                return backgroundJobServerStatusWriteModelSpy;
            }
        };
    }

    @Test
    void ifOperatingSystemMXBeanReturnsNaNForSystemCpuLoadOnFirstCall_NegativeIsReturned() {
        when(backgroundJobServerStatusWriteModelSpy.getMXBeanValue("SystemCpuLoad")).thenReturn(Double.NaN);

        serverZooKeeper.run();

        verify(storageProvider).announceBackgroundJobServer(backgroundJobServerStatusArgumentCaptor.capture());

        assertThat(backgroundJobServerStatusArgumentCaptor.getValue().getSystemCpuLoad()).isEqualTo(-1);
    }

    @Test
    void ifOperatingSystemMXBeanReturnsNaNForSystemCpuLoadOnLaterCalls_CachedValueIsReturned() {
        when(backgroundJobServerStatusWriteModelSpy.getMXBeanValue("SystemCpuLoad")).thenReturn(0.7, Double.NaN, 0.5);

        serverZooKeeper.run();
        verify(storageProvider).announceBackgroundJobServer(backgroundJobServerStatusArgumentCaptor.capture());
        assertThat(backgroundJobServerStatusArgumentCaptor.getValue().getSystemCpuLoad()).isEqualTo(0.7);

        serverZooKeeper.run();
        verify(storageProvider, times(2)).announceBackgroundJobServer(backgroundJobServerStatusArgumentCaptor.capture());
        assertThat(backgroundJobServerStatusArgumentCaptor.getValue().getSystemCpuLoad()).isEqualTo(0.7);

        serverZooKeeper.run();
        verify(storageProvider, times(3)).announceBackgroundJobServer(backgroundJobServerStatusArgumentCaptor.capture());
        assertThat(backgroundJobServerStatusArgumentCaptor.getValue().getSystemCpuLoad()).isEqualTo(0.5);
    }

    @Test
    void ifOperatingSystemMXBeanReturnsNaNForProcessCpuLoadOnFirstCall_NegativeIsReturned() {
        when(backgroundJobServerStatusWriteModelSpy.getMXBeanValue("ProcessCpuLoad")).thenReturn(Double.NaN);

        serverZooKeeper.run();

        verify(storageProvider).announceBackgroundJobServer(backgroundJobServerStatusArgumentCaptor.capture());

        assertThat(backgroundJobServerStatusArgumentCaptor.getValue().getProcessCpuLoad()).isEqualTo(-1);
    }

    @Test
    void ifOperatingSystemMXBeanReturnsNaNForProcessCpuLoadOnLaterCalls_CachedValueIsReturned() {
        when(backgroundJobServerStatusWriteModelSpy.getMXBeanValue("ProcessCpuLoad")).thenReturn(0.7, Double.NaN, 0.5);

        serverZooKeeper.run();
        verify(storageProvider).announceBackgroundJobServer(backgroundJobServerStatusArgumentCaptor.capture());
        assertThat(backgroundJobServerStatusArgumentCaptor.getValue().getProcessCpuLoad()).isEqualTo(0.7);

        serverZooKeeper.run();
        verify(storageProvider, times(2)).announceBackgroundJobServer(backgroundJobServerStatusArgumentCaptor.capture());
        assertThat(backgroundJobServerStatusArgumentCaptor.getValue().getProcessCpuLoad()).isEqualTo(0.7);

        serverZooKeeper.run();
        verify(storageProvider, times(3)).announceBackgroundJobServer(backgroundJobServerStatusArgumentCaptor.capture());
        assertThat(backgroundJobServerStatusArgumentCaptor.getValue().getProcessCpuLoad()).isEqualTo(0.5);
    }

}
