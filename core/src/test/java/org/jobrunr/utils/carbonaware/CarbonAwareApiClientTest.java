package org.jobrunr.utils.carbonaware;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.time.Instant.now;
import static java.time.Instant.parse;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.jobrunr.utils.carbonaware.DayAheadEnergyPricesAssert.assertThat;

class CarbonAwareApiClientTest extends AbstractCarbonAwareWiremockTest {

    CarbonAwareApiClient carbonAwareApiClient;

    @BeforeEach
    void setUp() {
        carbonAwareApiClient = createCarbonAwareApiClient();
    }

    @Test
    void testFetchLatestDayAheadEnergyPrices() {
        // GIVEN
        mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.BELGIUM_2024_03_12);

        // WHEN
        DayAheadEnergyPrices result = carbonAwareApiClient.fetchLatestDayAheadEnergyPrices(Optional.of("BE"));

        // THEN
        assertThat(result)
                .hasAreaCode("BE")
                .hasHourlyEnergyPricesSize(33)
                .hasHourlyEnergyPriceAt(0, parse("2024-03-12T03:00:00Z"), 64.23);
    }

    @Test
    void whenNoData_thenReturnErrorResponse() {
        // GIVEN
        mockResponseWhenRequestingAreaCode("DE", CarbonApiMockResponses.GERMANY_NO_DATA);
        CarbonAwarePeriod carbonAwarePeriod = CarbonAwarePeriod.before(now().plus(1, DAYS));

        // WHEN
        DayAheadEnergyPrices result = carbonAwareApiClient.fetchLatestDayAheadEnergyPrices(Optional.of("DE"));

        // THEN
        assertThat(result)
                .hasError("An error occurred: No data available for areaCode: 'DE'")
                .hasNoValidDataFor(carbonAwarePeriod);
    }

    @Test
    void whenMultipleConcurrentRequests_thenHandleAllCorrectly() throws InterruptedException, ExecutionException {
        // GIVEN
        mockResponseWhenRequestingAreaCode("multi-thread", CarbonApiMockResponses.BELGIUM_2024_03_12);

        // WHEN
        ExecutorService service = Executors.newFixedThreadPool(10);
        List<Future<DayAheadEnergyPrices>> futures = IntStream.range(0, 10)
                .mapToObj(i -> service.submit(() -> carbonAwareApiClient.fetchLatestDayAheadEnergyPrices(Optional.of("multi-thread"))))
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
    void whenInvalidJson_thenReturnErrorResponse() {
        // GIVEN
        mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.INVALID_JSON);

        // WHEN
        DayAheadEnergyPrices dayAheadEnergyPrices = carbonAwareApiClient.fetchLatestDayAheadEnergyPrices(Optional.of("BE"));

        // THEN
        assertThat(dayAheadEnergyPrices)
                .hasError("Error processing energy prices for area code 'BE': JobRunr encountered a problematic exception. Please create a bug report (if possible, provide the code to reproduce this and the stacktrace)");
    }

    @Test
    void whenExtraFieldInResponse_thenReturnErrorResponse() {
        // GIVEN
        mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.EXTRA_FIELD);

        // WHEN
        DayAheadEnergyPrices dayAheadEnergyPrices = carbonAwareApiClient.fetchLatestDayAheadEnergyPrices(Optional.of("BE"));

        // THEN
        assertThat(dayAheadEnergyPrices)
                .hasError("Error processing energy prices for area code 'BE': JobRunr encountered a problematic exception. Please create a bug report (if possible, provide the code to reproduce this and the stacktrace)");
    }

    @Test
    void whenMissingFieldsInResponse_thenFieldShouldBeNull() {
        // GIVEN
        mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.MISSING_UNIT_FIELD);

        // WHEN
        DayAheadEnergyPrices dayAheadEnergyPrices = carbonAwareApiClient.fetchLatestDayAheadEnergyPrices(Optional.of("BE"));

        // THEN
        assertThat(dayAheadEnergyPrices)
                .hasAreaCode("BE")
                .hasHourlyEnergyPricesSize(33)
                .hasNullUnit();
    }
}
