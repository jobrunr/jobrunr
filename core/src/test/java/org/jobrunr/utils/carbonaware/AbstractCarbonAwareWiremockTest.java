package org.jobrunr.utils.carbonaware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.utils.carbonaware.CarbonAwareConfiguration.*;
import static org.jobrunr.utils.carbonaware.CarbonAwareConfigurationReader.getCarbonAwareApiUrlPath;

public class AbstractCarbonAwareWiremockTest {

    private static WireMockServer wireMockServer;
    private static JsonMapper jsonMapper;
    private static final String carbonApiTestUrl = CarbonAwareConfigurationReader.getCarbonAwareApiUrl("http://localhost:10000");

    @BeforeAll
    static void beforeAll() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        jsonMapper = new JacksonJsonMapper(mapper);
        wireMockServer = new WireMockServer(10000);
        wireMockServer.start();
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
    }

    @AfterAll
    static void tearDown() {
        wireMockServer.stop();
    }

    protected CarbonAwareApiClient createCarbonAwareApiClient() {
        return new CarbonAwareApiClient(CarbonAwareConfigurationReader.getCarbonAwareApiUrl("http://localhost:10000"), DEFAULT_CLIENT_API_CONNECT_TIMEOUT, DEFAULT_CLIENT_API_READ_TIMEOUT, jsonMapper);
    }

    protected void mockResponseWhenRequestingAreaCode(String areaCode, String response) {
        String url = String.format(getCarbonAwareApiUrlPath() + "&areaCode=%s", areaCode);
        wireMockServer.stubFor(WireMock.get(urlEqualTo(url))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)));
    }

    protected BackgroundJobServer initializeJobRunr(int pollIntervalMs, String areaCode, StorageProvider storageProvider) {
        return JobRunr.configure()
                .useStorageProvider(storageProvider)
                .useCarbonAwareScheduling(usingStandardCarbonAwareConfiguration().andAreaCode(areaCode)
                        .andCarbonAwareApiUrl(carbonApiTestUrl))
                .useBackgroundJobServer(usingStandardBackgroundJobServerConfiguration().andPollInterval(Duration.ofMillis(pollIntervalMs)))
                .initialize()
                .getBackgroundJobServer();
    }
}
