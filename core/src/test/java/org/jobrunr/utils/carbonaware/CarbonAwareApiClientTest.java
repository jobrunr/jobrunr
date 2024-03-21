package org.jobrunr.utils.carbonaware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.mockito.CarbonAwareConfigurationMocker.mockCarbonAwareConf;

class CarbonAwareApiClientTest {
    private static WireMockServer wireMockServer;
    private static JsonMapper jsonMapper;
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

    @AfterEach
    void resetMockServer() {
        wireMockServer.resetAll();
    }

    @AfterAll
    static void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testFetchLatestDayAheadEnergyPrices() {
        // ARRANGE
        CarbonAwareApiClient carbonAwareApiClient = new CarbonAwareApiClient(jsonMapper);
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/carbon-intensity/v1/day-ahead-energy-prices?area=BE"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(CarbonApiMockResponses.BELGIUM_2024_03_12)));

        // ACT
        DayAheadEnergyPrices result;
        try(MockedStatic<CarbonAwareConfiguration> conf = mockCarbonAwareConf("BE")) {
            result = carbonAwareApiClient.fetchLatestDayAheadEnergyPrices(Optional.of("BE"));
        }

        // ASSERT
        assertThat(result.getHoursAvailable()).isEqualTo(33);
        assertThat(result.getArea()).isEqualTo("BE");
        assertThat(result.getHourlyEnergyPrices().size()).isEqualTo(33);
        DayAheadEnergyPrices.HourlyEnergyPrice cheapestPrice = result.getHourlyEnergyPrices().get(0);
        assertThat(cheapestPrice.getDateTime().toString()).isEqualTo("2024-03-12T03:00:00Z");
        assertThat(cheapestPrice.getPrice()).isEqualTo(64.23);
    }
}