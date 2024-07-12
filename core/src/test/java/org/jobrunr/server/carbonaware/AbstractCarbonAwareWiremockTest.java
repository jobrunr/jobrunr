package org.jobrunr.server.carbonaware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.jobrunr.server.carbonaware.CarbonAwareConfigurationReader.getCarbonIntensityForecastApiPath;

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
        carbonApiTestUrl = CarbonAwareConfigurationReader.getCarbonIntensityForecastApiUrl("http://localhost:" + wireMockRuntimeInfo.getHttpPort());
        WireMock.reset();
    }

    protected CarbonIntensityApiClient createCarbonAwareApiClient(String areaCode) {
        CarbonAwareConfiguration carbonAwareConfiguration = CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration()
                .andAreaCode(areaCode)
                .andCarbonIntensityApiUrl(carbonIntensityApiBaseUrl);

        return new CarbonIntensityApiClient(new CarbonAwareConfigurationReader(carbonAwareConfiguration), jsonMapper);
    }

    protected void mockResponseWhenRequestingAreaCode(String areaCode, String response) {
        String url = String.format(getCarbonIntensityForecastApiPath() + "?region=%s", areaCode);
        stubFor(WireMock.get(urlEqualTo(url))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)));
    }

    protected void mockResponseWhenRequestingAreaCodeWithScenarios(String areaCode, String... responses) {
        for (int i = 0; i < responses.length; i++) {
            String currentScenario = i == 0 ? Scenario.STARTED : "Scenario " + i;
            String nextScenario = i == responses.length - 1 ? Scenario.STARTED : "Scenario " + (i + 1);
            String url = String.format(getCarbonIntensityForecastApiPath() + "?region=%s", areaCode);
            stubFor(WireMock.get(urlEqualTo(url))
                    .inScenario("mock-responses")
                    .whenScenarioStateIs(currentScenario)
                    .willSetStateTo(nextScenario)
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody(responses[i])));
        }
    }

    protected void verifyApiCalls(String areaCode, int calls) {
        String url = String.format(getCarbonIntensityForecastApiPath() + "?region=%s", areaCode);
        WireMock.verify(calls, getRequestedFor(urlEqualTo(url)));
    }

}
