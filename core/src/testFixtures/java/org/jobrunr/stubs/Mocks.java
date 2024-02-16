package org.jobrunr.stubs;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.BackgroundJobServerConfigurationReader;
import org.mockito.Mockito;

import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.mockito.Mockito.lenient;

public class Mocks {

    public static BackgroundJobServer ofBackgroundJobServer() {
        return ofBackgroundJobServer(usingStandardBackgroundJobServerConfiguration());
    }

    public static BackgroundJobServer ofBackgroundJobServer(BackgroundJobServerConfiguration configuration) {
        BackgroundJobServer mock = Mockito.mock(BackgroundJobServer.class);
        BackgroundJobServerConfigurationReader configurationReader = new BackgroundJobServerConfigurationReader(configuration);
        lenient().when(mock.getId()).thenReturn(configurationReader.getId());
        lenient().when(mock.getConfiguration()).thenReturn(configurationReader);
        return mock;
    }
}
