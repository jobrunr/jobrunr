package org.jobrunr.utils.carbonaware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.utils.carbonaware.CarbonAwareSchedulingTestUtils.mockResponseWhenRequestingArea;
import static org.jobrunr.utils.carbonaware.CarbonAwareSchedulingTestUtils.mockCarbonAwareConf;

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
        mockResponseWhenRequestingArea("BE", CarbonApiMockResponses.BELGIUM_2024_03_12, wireMockServer);

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

    @Test
    void whenFetchLatestDayAheadEnergyPrices_andNoData_thenReturnErrorResponse() {
        // ARRANGE
        CarbonAwareApiClient carbonAwareApiClient = new CarbonAwareApiClient(jsonMapper);
        mockResponseWhenRequestingArea("DE", CarbonApiMockResponses.GERMANY_NO_DATA, wireMockServer);
        CarbonAwarePeriod carbonAwarePeriod = CarbonAwarePeriod.between(Instant.now(), Instant.now().plus(1, ChronoUnit.DAYS));

        // ACT
        DayAheadEnergyPrices result;
        try(MockedStatic<CarbonAwareConfiguration> conf = mockCarbonAwareConf("DE")) {
            result = carbonAwareApiClient.fetchLatestDayAheadEnergyPrices(Optional.of("DE"));
        }

        // ASSERT
        assertThat(result.hasValidData(carbonAwarePeriod)).isFalse();
        assertThat(result.getIsErrorResponse()).isTrue();
        assertThat(result.getErrorMessage()).isEqualTo("An error occurred: No data available for area: 'DE'");
        assertThat(result.getArea()).isNull();
        assertThat(result.getHoursAvailable()).isNull();
    }
}