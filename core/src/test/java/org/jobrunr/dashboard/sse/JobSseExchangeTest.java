package org.jobrunr.dashboard.sse;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.jobs.JobId;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aDeletedJob;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJob;
import static org.jobrunr.jobs.JobTestBuilder.aSucceededJob;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobSseExchangeTest {

    @Mock
    private HttpExchange httpExchange;
    @Mock
    private StorageProvider storageProvider;
    private UUID jobId;

    @BeforeEach
    void setUp() throws URISyntaxException {
        jobId = UUID.randomUUID();
        when(httpExchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());
        when(httpExchange.getResponseHeaders()).thenReturn(new Headers());
        when(httpExchange.getRequestURI()).thenReturn(new URI("/sse/jobs/" + jobId));
    }

    @Test
    void sseConnectionSubscribesForJobStates() throws IOException {
        JobSseExchange jobSseExchange = new JobSseExchange(httpExchange, storageProvider, new JacksonJsonMapper());
        verify(storageProvider).addJobStorageOnChangeListener(jobSseExchange);
    }

    @Test
    void sseConnectionParsesJobIdCorrectly() throws IOException {
        JobSseExchange jobSseExchange = new JobSseExchange(httpExchange, storageProvider, new JacksonJsonMapper());

        assertThat(jobSseExchange.getJobId()).isEqualTo(new JobId(jobId));
    }

    @Test
    void sseConnectionIsClosedIfJobStateIsSucceeded() throws IOException {
        JobSseExchange jobSseExchange = new JobSseExchange(httpExchange, storageProvider, new JacksonJsonMapper());

        jobSseExchange.onChange(aSucceededJob().build());

        verify(storageProvider).removeJobStorageOnChangeListener(jobSseExchange);
    }

    @Test
    void sseConnectionIsClosedIfJobStateIsDeleted() throws IOException {
        JobSseExchange jobSseExchange = new JobSseExchange(httpExchange, storageProvider, new JacksonJsonMapper());

        jobSseExchange.onChange(aDeletedJob().build());

        verify(storageProvider).removeJobStorageOnChangeListener(jobSseExchange);
    }

    @Test
    void sseConnectionIsClosedIfJobStateIsFailed() throws IOException {
        JobSseExchange jobSseExchange = new JobSseExchange(httpExchange, storageProvider, new JacksonJsonMapper());

        jobSseExchange.onChange(aFailedJob().build());

        verify(storageProvider).removeJobStorageOnChangeListener(jobSseExchange);
    }

    @Test
    void closeRemovesSseExchangeFromStorageProviderListeners() throws IOException {
        JobSseExchange jobSseExchange = new JobSseExchange(httpExchange, storageProvider, new JacksonJsonMapper());

        jobSseExchange.close();

        verify(storageProvider).removeJobStorageOnChangeListener(jobSseExchange);
    }

}