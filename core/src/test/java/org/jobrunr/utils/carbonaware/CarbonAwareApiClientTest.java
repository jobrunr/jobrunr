package org.jobrunr.utils.carbonaware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.*;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        DayAheadEnergyPrices result = carbonAwareApiClient.fetchLatestDayAheadEnergyPrices(Optional.of("BE"));

        // ASSERT
        assertEquals(33, result.getHoursAvailable());
        assertEquals("BE", result.getArea());
        assertEquals(33, result.getHourlyEnergyPrices().size());
        DayAheadEnergyPrices.HourlyEnergyPrice cheapestPrice = result.getHourlyEnergyPrices().get(0);
        assertEquals("2024-03-12T03:00:00Z", cheapestPrice.getDateTime().toString());
        assertEquals(64.23, cheapestPrice.getPrice());
    }
}