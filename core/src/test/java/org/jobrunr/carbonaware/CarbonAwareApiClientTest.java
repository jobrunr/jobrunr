package org.jobrunr.carbonaware;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.time.Instant.parse;
import static org.jobrunr.utils.carbonaware.DayAheadEnergyPricesAssert.assertThat;

class CarbonAwareApiClientTest extends AbstractCarbonAwareWiremockTest {

    @Test
    void testFetchLatestDayAheadEnergyPrices() {
        // GIVEN
        CarbonAwareApiClient carbonAwareApiClient = createCarbonAwareApiClient("BE");
        mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.BELGIUM_2024_03_12);

        // WHEN
        DayAheadEnergyPrices result = carbonAwareApiClient.fetchLatestDayAheadEnergyPrices();

        // THEN
        assertThat(result)
                .hasAreaCode("BE")
                .hasHourlyEnergyPricesSize(33)
                .hasHourlyEnergyPriceAt(0, parse("2024-03-12T03:00:00Z"), 64.23);
    }

    @Test
    void testFetchLatestDayAheadEnergyPricesCorrectlyHandleConcurrentRequests() throws InterruptedException, ExecutionException {
        // GIVEN
        CarbonAwareApiClient carbonAwareApiClient = createCarbonAwareApiClient("BE");
        mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.BELGIUM_2024_03_12);

        // WHEN
        ExecutorService service = Executors.newFixedThreadPool(10);
        List<Future<DayAheadEnergyPrices>> futures = IntStream.range(0, 10)
                .mapToObj(i -> service.submit(carbonAwareApiClient::fetchLatestDayAheadEnergyPrices))
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
    void testFetchLatestDayAheadEnergyPricesReturnsNullWhenNoApiResponseIsError() {
        // GIVEN
        CarbonAwareApiClient carbonAwareApiClient = createCarbonAwareApiClient("DE");
        mockResponseWhenRequestingAreaCode("DE", CarbonApiMockResponses.GERMANY_NO_DATA);

        // WHEN
        DayAheadEnergyPrices result = carbonAwareApiClient.fetchLatestDayAheadEnergyPrices();

        // THEN
        assertThat(result).hasNoData();
    }

    @Test
    void testFetchLatestDayAheadEnergyPricesSetsMissingFieldsToNull() {
        // GIVEN
        CarbonAwareApiClient carbonAwareApiClient = createCarbonAwareApiClient("BE");
        mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.MISSING_UNIT_FIELD);

        // WHEN
        DayAheadEnergyPrices dayAheadEnergyPrices = carbonAwareApiClient.fetchLatestDayAheadEnergyPrices();

        // THEN
        assertThat(dayAheadEnergyPrices)
                .hasAreaCode("BE")
                .hasHourlyEnergyPricesSize(33)
                .hasNullUnit();
    }

    @Test
    void testFetchLatestDayAheadEnergyPricesReturnsNullWhenParsingInvalidJson() {
        // GIVEN
        CarbonAwareApiClient carbonAwareApiClient = createCarbonAwareApiClient("BE");
        mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.INVALID_JSON);

        // WHEN
        DayAheadEnergyPrices dayAheadEnergyPrices = carbonAwareApiClient.fetchLatestDayAheadEnergyPrices();

        // THEN
        assertThat(dayAheadEnergyPrices).hasNoData();
    }

    @Test
    void testFetchLatestDayAheadEnergyPricesReturnsNullWhenParsingResponseWithExtraFields() {
        // GIVEN
        CarbonAwareApiClient carbonAwareApiClient = createCarbonAwareApiClient("BE");
        mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.EXTRA_FIELD);

        // WHEN
        DayAheadEnergyPrices dayAheadEnergyPrices = carbonAwareApiClient.fetchLatestDayAheadEnergyPrices();

        // THEN
        assertThat(dayAheadEnergyPrices).hasNoData();
    }
}
