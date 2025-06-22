package org.jobrunr.server.carbonaware;

import org.jobrunr.utils.mapper.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.time.Instant;

import static java.time.Instant.parse;
import static org.jobrunr.jobs.carbonaware.CarbonIntensityForecastAssert.assertThat;
import static org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfiguration.usingStandardCarbonAwareJobProcessingConfiguration;

abstract class AbstractCarbonIntensityApiClientTest {

    @RegisterExtension
    static CarbonAwareApiWireMockExtension carbonAwareWiremock = new CarbonAwareApiWireMockExtension();

    protected abstract JsonMapper getJsonMapper();

    @Test
    void fetchCarbonIntensityForecast() {
        // GIVEN
        carbonAwareWiremock.mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.BELGIUM_2024_07_11);
        CarbonIntensityApiClient carbonIntensityApiClient = createCarbonAwareApiClient("BE");

        // WHEN
        CarbonIntensityForecast result = carbonIntensityApiClient.fetchCarbonIntensityForecast();

        // THEN
        assertThat(result)
                .hasDisplayName("Belgium")
                .hasForecastInterval(Duration.ofHours(1))
                .hasNextForecastAvailableAt(Instant.parse("2024-07-11T16:30:00.054245Z"))
                .hasIntensityForecastSize(24)
                .hasIntensityForecastAt(0, parse("2024-07-10T22:00:00Z"), 16);
    }

    @Test
    void fetchCarbonIntensityForecastReturnsNotOkWhenAreaNotFoundError() {
        // GIVEN
        carbonAwareWiremock.mockResponseWhenRequestingAreaCode("UNKNOWN", CarbonApiMockResponses.UNKNOWN_AREA);
        CarbonIntensityApiClient carbonIntensityApiClient = createCarbonAwareApiClient("UNKNOWN");

        // WHEN
        CarbonIntensityForecast result = carbonIntensityApiClient.fetchCarbonIntensityForecast();

        // THEN
        assertThat(result)
                .hasNoForecast()
                .hasForecastInterval(null)
                .hasNextForecastAvailableAt(null)
                .hasError()
                .hasErrorCode("DATA_PROVIDER_AREA_NOT_FOUND")
                .hasErrorMessage("No DataProvider supports area UNKNOWN.");
    }

    @Test
    void fetchCarbonIntensityForecastReturnsNotOWhenForecastNotAvailableError() {
        // GIVEN
        carbonAwareWiremock.mockResponseWhenRequestingAreaCode("DE", CarbonApiMockResponses.GERMANY_NO_DATA);
        CarbonIntensityApiClient carbonIntensityApiClient = createCarbonAwareApiClient("DE");

        // WHEN
        CarbonIntensityForecast result = carbonIntensityApiClient.fetchCarbonIntensityForecast();

        // THEN
        assertThat(result)
                .hasNoForecast()
                .hasForecastInterval(null)
                .hasNextForecastAvailableAt(null)
                .hasError()
                .hasErrorCode("FORECAST_NOT_AVAILABLE")
                .hasErrorMessage("No forecast available for DataProvider ENTSO-E and area Germany.");
    }

    @Test
    void fetchCarbonIntensityForecastSetsMissingFieldsToNull() {
        // GIVEN
        carbonAwareWiremock.mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.MISSING_FIELDS);
        CarbonIntensityApiClient carbonIntensityApiClient = createCarbonAwareApiClient("BE");

        // WHEN
        CarbonIntensityForecast carbonIntensityForecast = carbonIntensityApiClient.fetchCarbonIntensityForecast();

        // THEN
        assertThat(carbonIntensityForecast)
                .hasDisplayName("Belgium")
                .hasForecastInterval(null)
                .hasNextForecastAvailableAt(null)
                .hasIntensityForecastSize(24);
    }

    @Test
    void fetchCarbonIntensityForecastReturnsEmptyForecastWhenParsingOfResponseFails() {
        // GIVEN
        carbonAwareWiremock.mockResponseWhenRequestingAreaCode("BE", "{ someUnKnownKey: 'someValue' }");
        CarbonIntensityApiClient carbonIntensityApiClient = createCarbonAwareApiClient("BE");

        // WHEN
        CarbonIntensityForecast carbonIntensityForecast = carbonIntensityApiClient.fetchCarbonIntensityForecast();

        // THEN
        assertThat(carbonIntensityForecast).hasNoForecast();
    }

    protected CarbonIntensityApiClient createCarbonAwareApiClient(String areaCode) {
        var config = carbonAwareWiremock.getCarbonAwareJobProcessingConfigurationForAreaCode(areaCode);
        return new CarbonIntensityApiClient(new CarbonAwareJobProcessingConfigurationReader(config), getJsonMapper());
    }
}
