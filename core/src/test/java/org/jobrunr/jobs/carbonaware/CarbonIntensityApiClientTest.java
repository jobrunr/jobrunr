package org.jobrunr.jobs.carbonaware;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.time.Instant.parse;
import static org.jobrunr.jobs.carbonaware.DayAheadEnergyPricesAssert.assertThat;

class CarbonIntensityApiClientTest extends AbstractCarbonAwareWiremockTest {

    @Test
    void testFetchLatestDayAheadEnergyPrices() {
        // GIVEN
        CarbonIntensityApiClient carbonIntensityApiClient = createCarbonAwareApiClient("BE");
        mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.BELGIUM_2024_03_12);

        // WHEN
        DayAheadEnergyPrices result = carbonIntensityApiClient.fetchLatestDayAheadEnergyPrices();

        // THEN
        assertThat(result)
                .hasAreaCode("BE")
                .hasHourlyEnergyPricesSize(33)
                .hasHourlyEnergyPriceAt(0, parse("2024-03-12T03:00:00Z"), 64.23);
    }

    @Test
    void testFetchLatestDayAheadEnergyPricesCorrectlyHandleConcurrentRequests() throws InterruptedException, ExecutionException {
        // GIVEN
        CarbonIntensityApiClient carbonIntensityApiClient = createCarbonAwareApiClient("BE");
        mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.BELGIUM_2024_03_12);

        // WHEN
        ExecutorService service = Executors.newFixedThreadPool(10);
        List<Future<DayAheadEnergyPrices>> futures = IntStream.range(0, 10)
                .mapToObj(i -> service.submit(carbonIntensityApiClient::fetchLatestDayAheadEnergyPrices))
                .collect(Collectors.toList());

        // THEN
        for (Future<DayAheadEnergyPrices> future : futures) {
            DayAheadEnergyPrices result = future.get();
            assertThat(result)
                    .isNotNull()
                    .hasHourlyEnergyPricesSize(33);
        }

        service.shutdown();
    }

    @Test
    void testFetchLatestDayAheadEnergyPricesReturnsErrorDayAheadPricesWhenApiResponseIsDayAheadPricesNotFoundError() {
        // GIVEN
        CarbonIntensityApiClient carbonIntensityApiClient = createCarbonAwareApiClient("UNKNOWN");
        mockResponseWhenRequestingAreaCode("UNKNOWN", CarbonApiMockResponses.UNKNOWN_AREA);

        // WHEN
        DayAheadEnergyPrices result = carbonIntensityApiClient.fetchLatestDayAheadEnergyPrices();

        // THEN
        assertThat(result)
                .hasNoData()
                .hasError()
                .hasErrorCode("AREA_CODE_NOT_FOUND")
                .hasErrorMessage("AreaMapping not found for areaCode: UNKNOWN");
    }

    @Test
    void testFetchLatestDayAheadEnergyPricesReturnsErrorDayAheadPricesWhenApiResponseIsAreaNotFoundError() {
        // GIVEN
        CarbonIntensityApiClient carbonIntensityApiClient = createCarbonAwareApiClient("DE");
        mockResponseWhenRequestingAreaCode("DE", CarbonApiMockResponses.GERMANY_NO_DATA);

        // WHEN
        DayAheadEnergyPrices result = carbonIntensityApiClient.fetchLatestDayAheadEnergyPrices();

        // THEN
        assertThat(result)
                .hasNoData()
                .hasError()
                .hasErrorCode("DAY_AHEAD_PRICES_NOT_FOUND")
                .hasErrorMessage("No data available for areaCode: 'DE'");
    }

    @Test
    void testFetchLatestDayAheadEnergyPricesSetsMissingFieldsToNull() {
        // GIVEN
        CarbonIntensityApiClient carbonIntensityApiClient = createCarbonAwareApiClient("BE");
        mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.MISSING_STATE_FIELD);

        // WHEN
        DayAheadEnergyPrices dayAheadEnergyPrices = carbonIntensityApiClient.fetchLatestDayAheadEnergyPrices();

        // THEN
        assertThat(dayAheadEnergyPrices)
                .hasAreaCode("BE")
                .hasState(null)
                .hasHourlyEnergyPricesSize(33);
    }

    @Test
    void testFetchLatestDayAheadEnergyPricesReturnsEmptyDayAheadPricesWhenParsingInvalidJson() {
        // GIVEN
        CarbonIntensityApiClient carbonIntensityApiClient = createCarbonAwareApiClient("BE");
        mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.INVALID_JSON);

        // WHEN
        DayAheadEnergyPrices dayAheadEnergyPrices = carbonIntensityApiClient.fetchLatestDayAheadEnergyPrices();

        // THEN
        assertThat(dayAheadEnergyPrices).hasNoData();
    }

    @Test
    void testFetchLatestDayAheadEnergyPricesReturnsEmptyDayAheadPricesWhenParsingResponseWithExtraFields() {
        // GIVEN
        CarbonIntensityApiClient carbonIntensityApiClient = createCarbonAwareApiClient("BE");
        mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.EXTRA_FIELD);

        // WHEN
        DayAheadEnergyPrices dayAheadEnergyPrices = carbonIntensityApiClient.fetchLatestDayAheadEnergyPrices();

        // THEN
        assertThat(dayAheadEnergyPrices).hasNoData();
    }
}
