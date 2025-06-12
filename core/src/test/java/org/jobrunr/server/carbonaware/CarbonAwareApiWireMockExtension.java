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
import org.mockito.internal.util.reflection.Whitebox;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static java.lang.String.format;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfiguration.usingStandardCarbonAwareJobProcessingConfiguration;
import static org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfigurationReader.getCarbonIntensityForecastApiPath;
import static org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfigurationReader.getCarbonIntensityForecastApiRootUrl;

public class CarbonAwareApiWireMockExtension implements Extension, BeforeEachCallback {

    private static final WireMockServer wireMockServer;
    private final JsonMapper jsonMapper;
    protected final String carbonIntensityApiBaseUrl;

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
        this.jsonMapper = jsonMapper;
    }

    public CarbonAwareJobProcessingConfiguration getCarbonAwareJobProcessingConfigurationForAreaCode(String areaCode) {
        var config = usingStandardCarbonAwareJobProcessingConfiguration()
                .andApiClientRetriesOnException(1)
                .andAreaCode(areaCode);
        Whitebox.setInternalState(config, "carbonIntensityApiUrl", getCarbonIntensityForecastApiRootUrl(carbonIntensityApiBaseUrl));
        return config;
    }

    public void mockDefaultResponseWhenRequestingAreaCode(String areaCode) {
        mockResponseWhenRequestingAreaCode(areaCode, generateCarbonIntensityForecastForTheNextDay());
    }

    public void mockResponseWhenRequestingAreaCode(String areaCode, CarbonIntensityForecast forecast) {
        mockResponseWhenRequestingAreaCode(areaCode, jsonMapper.serialize(forecast));
    }

    public void mockResponseWhenRequestingAreaCode(String areaCode, String response) {
        var url = format(getCarbonIntensityForecastApiPath() + "?region=%s", areaCode);
        stubFor(WireMock.get(urlEqualTo(url))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)));
    }

    private CarbonIntensityForecast generateCarbonIntensityForecastForTheNextDay() {
        ZonedDateTime currentTime = ZonedDateTime.now().truncatedTo(HOURS);
        ZonedDateTime forecastUntil = currentTime.plusDays(1).with(LocalTime.MAX);
        List<TimestampedCarbonIntensityForecast> forecast = buildForecastSlots(currentTime, forecastUntil, HOURS, i -> i);
        ZonedDateTime nextDataAvailableAt = forecastUntil.withHour(18).withMinute(30).truncatedTo(MINUTES);
        return generateCarbonIntensityForecastUsing(nextDataAvailableAt, forecast);
    }

    public static CarbonIntensityForecast generateCarbonIntensityForecastUsing(ZonedDateTime nextDataAvailableAt, List<TimestampedCarbonIntensityForecast> forecast) {
        return new CarbonIntensityForecast(
                new ApiResponseStatus("OK", "DataProvider MOCK_PROVIDER and area MOCK_AREA has " + forecast.size() + " forecasts."),
                "MOCK_PROVIDER", "MOCK_ID", "MOCK_AREA",
                systemDefault().getId(), nextDataAvailableAt.toInstant(),
                Duration.between(forecast.get(0).getPeriodStartAt(), forecast.get(0).getPeriodEndAt()),
                forecast);
    }

    /**
     * Builds a consecutive list of TimestampedCarbonIntensityForecast slots,
     * starting at 'start', ending at 'end' with slots of length 'slotUnit',
     * where each slot’s “value” comes from intensityFn.apply(i).
     */
    public static List<TimestampedCarbonIntensityForecast> buildForecastSlots(ZonedDateTime start, ZonedDateTime end, TemporalUnit slotUnit, IntFunction<Integer> intensityFn) {
        int rangeStart = 0;
        int rangeEnd = (int) slotUnit.between(start, end);
        return IntStream.range(rangeStart, rangeEnd + 1) // range is endExclusive
                .mapToObj(i -> {
                    Instant slotStart = start.toInstant().plus(i, slotUnit);
                    Instant slotEnd = start.toInstant().plus(i + 1, slotUnit);
                    int value = intensityFn.apply(i);
                    return new TimestampedCarbonIntensityForecast(slotStart, slotEnd, value);
                })
                .collect(toList());
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        WireMock.reset();
    }
}
