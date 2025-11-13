package org.jobrunr.dashboard.sse;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.JobRunrAssertions.assertThatJson;
import static org.jobrunr.JobRunrAssertions.contentOfResource;
import static org.jobrunr.jobs.JobTestBuilder.aDeletedJob;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJob;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;
import static org.jobrunr.jobs.JobTestBuilder.aScheduledJob;
import static org.jobrunr.jobs.JobTestBuilder.aSucceededJob;
import static org.mockito.InstantMocker.FIXED_INSTANT_RIGHT_AFTER_THE_HOUR;
import static org.mockito.InstantMocker.mockTime;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractJobStatsSseExchangeTest {
    private ByteArrayOutputStream outStream;
    private StorageProvider storageProvider;

    @Mock
    private HttpExchange httpExchange;

    @BeforeEach
    void setUp() {
        this.storageProvider = getStorageProvider();
        saveTestJobs();

        this.outStream = new ByteArrayOutputStream();
        when(httpExchange.getResponseBody()).thenReturn(outStream);
        when(httpExchange.getResponseHeaders()).thenReturn(new Headers());
    }

    @Test
    void onChangeSendsJobStats() throws IOException {
        try (var ignored = mockTime(FIXED_INSTANT_RIGHT_AFTER_THE_HOUR)) {
            var sseExchange = new JobStatsSseExchange(httpExchange, storageProvider, jsonMapper());
            outStream.reset();

            sseExchange.onChange(storageProvider.getJobStats());
            var eventData = outStream.toString(StandardCharsets.UTF_8).trim();

            assertThat(eventData).startsWith("event\ndata:");
            assertThatJson(eventData.replace("event\ndata:", "")).isEqualTo(contentOfResource("/dashboard/sse/job-stats.json"));
        }
    }

    protected abstract JsonMapper jsonMapper();

    private StorageProvider getStorageProvider() {
        var storageProvider = new InMemoryStorageProvider();
        storageProvider.setJobMapper(new JobMapper(jsonMapper()));
        return storageProvider;
    }

    private void saveTestJobs() {
        // special save to test all-time-succeeded
        storageProvider.save(aSucceededJob().withDeletedState().build());
        storageProvider.save(asList(
                aSucceededJob().withDeletedState().build(),
                aSucceededJob().withDeletedState().build()));

        // all other jobs
        storageProvider.save(asList(
                aJobInProgress().build(),
                aScheduledJob().build(),
                aFailedJob().build(),
                aFailedJob().build(),
                aSucceededJob().build(),
                aDeletedJob().build()
        ));
    }
}
