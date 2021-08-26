package org.jobrunr.server.tasks;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckForNewJobRunrVersionTest {

    @Mock
    BackgroundJobServer backgroundJobServer;

    @BeforeEach
    public void setUpBackgroundJobServer() {
        when(backgroundJobServer.getJsonMapper()).thenReturn(new JacksonJsonMapper());
    }

    @Test
    void test() {
        new CheckForNewJobRunrVersion(backgroundJobServer).run();
    }

}