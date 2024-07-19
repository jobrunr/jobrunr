package org.jobrunr.server.carbonaware;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static java.time.Instant.parse;
import static org.jobrunr.jobs.carbonaware.CarbonIntensityForecastAssert.assertThat;

class CarbonIntensityApiClientTest extends AbstractCarbonAwareWiremockTest {

    @Test
    void testFetchCarbonIntensityForecast() {
        // GIVEN
        CarbonIntensityApiClient carbonIntensityApiClient = createCarbonAwareApiClient("BE");
        mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.BELGIUM_2024_07_11);

        // WHEN
        CarbonIntensityForecast result = carbonIntensityApiClient.fetchCarbonIntensityForecast();

        // THEN
        assertThat(result)
                .hasDisplayName("Belgium")
                .hasMinimumScheduleMargin(Duration.ofHours(3))
                .hasNextForecastAvailableAt(Instant.parse("2024-07-11T16:30:00.054245Z"))
                .hasIntensityForecastSize(24)
                .hasIntensityForecastAt(0, parse("2024-07-10T22:00:00Z"), 16);
    }

    @Test
    void testFetchCarbonIntensityForecastReturnsNotOkApiResponseStatusWhenApiResponseIsResponseIsAreaNotFoundError() {
        // GIVEN
        CarbonIntensityApiClient carbonIntensityApiClient = createCarbonAwareApiClient("UNKNOWN");
        mockResponseWhenRequestingAreaCode("UNKNOWN", CarbonApiMockResponses.UNKNOWN_AREA);

        // WHEN
        CarbonIntensityForecast result = carbonIntensityApiClient.fetchCarbonIntensityForecast();

        // THEN
        assertThat(result)
                .hasNoForecast()
                .hasMinimumScheduleMargin(null)
                .hasNextForecastAvailableAt(null)
                .hasError()
                .hasErrorCode("DATA_PROVIDER_AREA_NOT_FOUND")
                .hasErrorMessage("No DataProvider supports area UNKNOWN.");
    }

    @Test
    void testFetchCarbonIntensityForecastReturnsNotOkApiResponseStatusWhenApiResponseIsForecastNotAvailableError() {
        // GIVEN
        CarbonIntensityApiClient carbonIntensityApiClient = createCarbonAwareApiClient("DE");
        mockResponseWhenRequestingAreaCode("DE", CarbonApiMockResponses.GERMANY_NO_DATA);

        // WHEN
        CarbonIntensityForecast result = carbonIntensityApiClient.fetchCarbonIntensityForecast();

        // THEN
        assertThat(result)
                .hasNoForecast()
                .hasMinimumScheduleMargin(null)
                .hasNextForecastAvailableAt(null)
                .hasError()
                .hasErrorCode("FORECAST_NOT_AVAILABLE")
                .hasErrorMessage("No forecast available for DataProvider ENTSO-E and area Germany.");
    }

    @Test
    void testFetchCarbonIntensityForecastSetsMissingFieldsToNull() {
        // GIVEN
        CarbonIntensityApiClient carbonIntensityApiClient = createCarbonAwareApiClient("BE");
        mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.MISSING_FIELDS);

        // WHEN
        CarbonIntensityForecast carbonIntensityForecast = carbonIntensityApiClient.fetchCarbonIntensityForecast();

        // THEN
        assertThat(carbonIntensityForecast)
                .hasDisplayName("Belgium")
                .hasMinimumScheduleMargin(null)
                .hasNextForecastAvailableAt(null)
                .hasIntensityForecastSize(24);
    }

    @Test
    void testFetchCarbonIntensityForecastReturnsEmptyCarbonIntensityForecastWhenParsingResponseWithExtraFields() {
        // GIVEN
        CarbonIntensityApiClient carbonIntensityApiClient = createCarbonAwareApiClient("BE");
        mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.EXTRA_FIELD);

        // WHEN
        CarbonIntensityForecast carbonIntensityForecast = carbonIntensityApiClient.fetchCarbonIntensityForecast();

        // THEN
        assertThat(carbonIntensityForecast).hasNoForecast();
    }
}
