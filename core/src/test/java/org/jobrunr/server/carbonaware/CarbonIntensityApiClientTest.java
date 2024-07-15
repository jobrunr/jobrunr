package org.jobrunr.server.carbonaware;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
                .hasIntensityForecastSize(24)
                .hasIntensityForecastAt(0, parse("2024-07-10T22:00:00Z"), 16);
    }

    @Test
    void testFetchCarbonIntensityForecastCorrectlyHandleConcurrentRequests() throws InterruptedException, ExecutionException {
        // GIVEN
        CarbonIntensityApiClient carbonIntensityApiClient = createCarbonAwareApiClient("BE");
        mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.BELGIUM_2024_07_11);

        // WHEN
        ExecutorService service = Executors.newFixedThreadPool(10);
        List<Future<CarbonIntensityForecast>> futures = IntStream.range(0, 10)
                .mapToObj(i -> service.submit(carbonIntensityApiClient::fetchCarbonIntensityForecast))
                .collect(Collectors.toList());

        // THEN
        for (Future<CarbonIntensityForecast> future : futures) {
            CarbonIntensityForecast result = future.get();
            assertThat(result)
                    .isNotNull()
                    .hasIntensityForecastSize(24);
        }

        service.shutdown();
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
                .hasError()
                .hasErrorCode("FORECAST_NOT_AVAILABLE")
                .hasErrorMessage("No forecast available for DataProvider ENTSO-E and area Germany.");
    }

    @Test
    void testFetchCarbonIntensityForecastSetsMissingFieldsToNull() {
        // GIVEN
        CarbonIntensityApiClient carbonIntensityApiClient = createCarbonAwareApiClient("BE");
        mockResponseWhenRequestingAreaCode("BE", CarbonApiMockResponses.MISSING_STATE_FIELD);

        // WHEN
        CarbonIntensityForecast carbonIntensityForecast = carbonIntensityApiClient.fetchCarbonIntensityForecast();

        // THEN
        assertThat(carbonIntensityForecast)
                .hasDisplayName("Belgium")
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
