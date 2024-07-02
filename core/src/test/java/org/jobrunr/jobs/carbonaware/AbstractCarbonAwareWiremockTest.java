package org.jobrunr.jobs.carbonaware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.jobrunr.jobs.carbonaware.CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration;
import static org.jobrunr.jobs.carbonaware.CarbonAwareConfigurationReader.getCarbonIntensityDayAheadEnergyPricesApiPath;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;

@WireMockTest
public abstract class AbstractCarbonAwareWiremockTest {

    protected static JsonMapper jsonMapper;

    protected String carbonIntensityApiBaseUrl;
    protected String carbonApiTestUrl;

    @BeforeAll
    static void beforeAll() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        jsonMapper = new JacksonJsonMapper(mapper);
    }

    @BeforeEach
    void setUp(WireMockRuntimeInfo wireMockRuntimeInfo) {
        carbonIntensityApiBaseUrl = "http://localhost:" + wireMockRuntimeInfo.getHttpPort();
        carbonApiTestUrl = CarbonAwareConfigurationReader.getCarbonIntensityDayAheadEnergyPricesApiUrl("http://localhost:" + wireMockRuntimeInfo.getHttpPort());
    }

    protected CarbonIntensityApiClient createCarbonAwareApiClient(String areaCode) {
        CarbonAwareConfiguration carbonAwareConfiguration = CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration()
                .andAreaCode(areaCode)
                .andCarbonIntensityApiUrl(carbonIntensityApiBaseUrl);

        return new CarbonIntensityApiClient(new CarbonAwareConfigurationReader(carbonAwareConfiguration), jsonMapper);
    }

    protected void mockResponseWhenRequestingAreaCode(String areaCode, String response) {
        String url = String.format(getCarbonIntensityDayAheadEnergyPricesApiPath() + "?areaCode=%s&state=", areaCode);
        stubFor(WireMock.get(urlEqualTo(url))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)));
    }

    protected void initializeJobRunr(int pollIntervalMs, String areaCode, StorageProvider storageProvider) {
        JobRunr.configure()
                .useStorageProvider(storageProvider)
                .useCarbonAwareScheduling(usingStandardCarbonAwareConfiguration().andAreaCode(areaCode)
                        .andCarbonIntensityApiUrl(carbonApiTestUrl))
                .useBackgroundJobServer(usingStandardBackgroundJobServerConfiguration().andPollInterval(Duration.ofMillis(pollIntervalMs)))
                .initialize();
    }

}
