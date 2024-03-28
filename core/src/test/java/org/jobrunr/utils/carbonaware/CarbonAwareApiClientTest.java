package org.jobrunr.utils.carbonaware;

import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static java.time.Instant.now;
import static java.time.Instant.parse;
import static org.jobrunr.utils.carbonaware.DayAheadEnergyPricesAssert.assertThat;

class CarbonAwareApiClientTest extends AbstractCarbonAwareWiremockTest {

    @Test
    void testFetchLatestDayAheadEnergyPrices() {
        // GIVEN
        CarbonAwareApiClient carbonAwareApiClient = createCarbonAwareApiClient();
        mockResponseWhenRequestingArea("BE", CarbonApiMockResponses.BELGIUM_2024_03_12);

        // WHEN
        DayAheadEnergyPrices result = carbonAwareApiClient.fetchLatestDayAheadEnergyPrices(Optional.of("BE"));

        // THEN
        assertThat(result)
            .hasArea("BE")
            .hasHourlyEnergyPricesSize(33)
            .hasHourlyEnergyPriceAt(0, parse("2024-03-12T03:00:00Z"), 64.23);
    }

    @Test
    void whenFetchLatestDayAheadEnergyPrices_andNoData_thenReturnErrorResponse() {
        // GIVEN
        CarbonAwareApiClient carbonAwareApiClient = createCarbonAwareApiClient();
        mockResponseWhenRequestingArea("DE", CarbonApiMockResponses.GERMANY_NO_DATA);
        CarbonAwarePeriod carbonAwarePeriod = CarbonAwarePeriod.before(now().plus(1, ChronoUnit.DAYS));

        // WHEN
        DayAheadEnergyPrices result = carbonAwareApiClient.fetchLatestDayAheadEnergyPrices(Optional.of("DE"));

        // THEN
        assertThat(result)
                .hasError("An error occurred: No data available for area: 'DE'")
                .hasNoValidDataFor(carbonAwarePeriod);
    }

}