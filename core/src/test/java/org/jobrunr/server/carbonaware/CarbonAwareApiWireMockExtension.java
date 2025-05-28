package org.jobrunr.server.carbonaware;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.jobrunr.server.carbonaware.CarbonIntensityForecast.ApiResponseStatus;
import org.jobrunr.server.carbonaware.CarbonIntensityForecast.TimestampedCarbonIntensityForecast;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfiguration.usingStandardCarbonAwareJobProcessingConfiguration;
import static org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfigurationReader.getCarbonIntensityForecastApiPath;

public class CarbonAwareApiWireMockExtension implements Extension, BeforeEachCallback {

    private static final WireMockServer wireMockServer;
    private final JsonMapper jsonMapper;
    protected final String carbonIntensityApiBaseUrl;
    protected final String carbonApiTestUrl;

    static {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor(wireMockServer.port());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            wireMockServer.stop();
            wireMockServer.shutdown();
        }));
    }

    public CarbonAwareApiWireMockExtension() {
        this(wireMockServer, new JacksonJsonMapper());
    }

    public CarbonAwareApiWireMockExtension(WireMockServer wireMockServer, JsonMapper jsonMapper) {
        this.carbonIntensityApiBaseUrl = "http://localhost:" + wireMockServer.port();
        this.carbonApiTestUrl = CarbonAwareJobProcessingConfigurationReader.getCarbonIntensityForecastApiUrl(carbonIntensityApiBaseUrl);
        this.jsonMapper = jsonMapper;
        WireMock.reset();
    }

    public CarbonAwareJobProcessingConfiguration getCarbonAwareJobProcessingConfigurationForAreaCode(String areaCode) {
        return usingStandardCarbonAwareJobProcessingConfiguration()
                .andCarbonIntensityApiUrl(carbonIntensityApiBaseUrl)
                .andApiClientRetriesOnException(1)
                .andAreaCode(areaCode);
    }

    public void mockResponseWhenRequestingAreaCode(String areaCode) {
        mockResponseWhenRequestingAreaCode(areaCode, generateCarbonIntensityForecastForTheNextDay());
    }

    public void mockResponseWhenRequestingAreaCode(String areaCode, String response) {
        String url = format(getCarbonIntensityForecastApiPath() + "?region=%s", areaCode);
        stubFor(WireMock.get(urlEqualTo(url))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)));
    }

    public Instant toEndOfNextDay(ZonedDateTime dateTime) {
        return dateTime.toLocalDate().atTime(LocalTime.MAX).plusDays(1).atZone(ZoneId.systemDefault()).toInstant();
    }

    private String generateCarbonIntensityForecastForTheNextDay() {
        ZonedDateTime currentTime = ZonedDateTime.now();
        Instant startingInstant = currentTime.toInstant().truncatedTo(HOURS);
        long limit = Duration.between(startingInstant, toEndOfNextDay(currentTime)).toHours() + 1;
        List<TimestampedCarbonIntensityForecast> forecast = Stream.iterate(0, i -> i + 1).limit(limit).map(
                i -> new TimestampedCarbonIntensityForecast(startingInstant.plus(i, HOURS), startingInstant.plus(i + 1, HOURS), i)
        ).collect(Collectors.toList());

        return jsonMapper.serialize(
                new CarbonIntensityForecast(
                        new ApiResponseStatus("OK", "DataProvider MOCK_PROVIDER and area MOCK_AREA has 24 forecasts."),
                        "MOCK_PROVIDER", "MOCK_ID", "MOCK_AREA",
                        ZoneId.systemDefault().getId(), currentTime.truncatedTo(DAYS).plusDays(1).withHour(18).withMinute(30).toInstant(),
                        Duration.ofHours(1),
                        forecast
                )
        );
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        WireMock.reset();
    }
}
