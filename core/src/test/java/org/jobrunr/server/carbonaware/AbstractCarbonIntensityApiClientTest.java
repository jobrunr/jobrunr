package org.jobrunr.server.carbonaware;

import ch.qos.logback.LoggerAssert;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Body;
import org.jobrunr.JobRunrAssertions;
import org.jobrunr.utils.mapper.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.lang.String.format;
import static java.time.Instant.parse;
import static org.jobrunr.jobs.carbonaware.CarbonIntensityForecastAssert.assertThat;
import static org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfigurationReader.getCarbonIntensityForecastApiPath;

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
    void fetchCarbonIntensityForecastWithResponseCode404SetsResponseStatus() {
        var url = format(getCarbonIntensityForecastApiPath() + "?region=BE");
        stubFor(WireMock.get(urlEqualTo(url)).willReturn(notFound().withResponseBody(new Body("<html><body>404 Not Found</body></html>"))));
        var apiClient = createCarbonAwareApiClient("BE");
        var logger =  LoggerAssert.initFor(apiClient);

        var forecast = apiClient.fetchCarbonIntensityForecast();

        assertThat(forecast)
                .hasErrorCode("404")
                .hasErrorMessage("HTTP Response Code 404");

        JobRunrAssertions.assertThat(logger)
                .hasErrorMessageContaining("Carbon Aware API call resulted in an error with code: '404' and message: '<html><body>404 Not Found</body></html>'");
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
