package org.jobrunr.server.carbonaware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.jobrunr.server.carbonaware.CarbonIntensityForecast.ApiResponseStatus;
import org.jobrunr.server.carbonaware.CarbonIntensityForecast.TimestampedCarbonIntensityForecast;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

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
import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.jobrunr.server.carbonaware.CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration;
import static org.jobrunr.server.carbonaware.CarbonAwareConfigurationReader.getCarbonIntensityForecastApiPath;

@WireMockTest
public abstract class AbstractCarbonAwareWiremockTest {

    protected static JsonMapper jsonMapper;

    protected String carbonIntensityApiBaseUrl;
    protected String carbonApiTestUrl;

    @BeforeAll
    static void beforeAll() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        jsonMapper = new JacksonJsonMapper(mapper);
    }

    @BeforeEach
    void setUp(WireMockRuntimeInfo wireMockRuntimeInfo) {
        carbonIntensityApiBaseUrl = "http://localhost:" + wireMockRuntimeInfo.getHttpPort();
        carbonApiTestUrl = CarbonAwareConfigurationReader.getCarbonIntensityForecastApiUrl("http://localhost:" + wireMockRuntimeInfo.getHttpPort());
        WireMock.reset();
    }

    protected CarbonIntensityApiClient createCarbonAwareApiClient(String areaCode) {
        CarbonAwareConfiguration carbonAwareConfiguration = usingStandardCarbonAwareConfiguration()
                .andAreaCode(areaCode)
                .andCarbonIntensityApiUrl(carbonIntensityApiBaseUrl);

        return new CarbonIntensityApiClient(new CarbonAwareConfigurationReader(carbonAwareConfiguration), jsonMapper);
    }

    protected void mockResponseWhenRequestingAreaCode(String areaCode, String response) {
        String url = format(getCarbonIntensityForecastApiPath() + "?region=%s", areaCode);
        stubFor(WireMock.get(urlEqualTo(url))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)));
    }

    protected void mockResponseWhenRequestingAreaCode(String areaCode) {
        String url = format(getCarbonIntensityForecastApiPath() + "?region=%s", areaCode);
        stubFor(WireMock.get(urlEqualTo(url))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(generateCarbonIntensityForecastForTheNextDay())));
    }

    protected CarbonAwareConfiguration getCarbonAwareConfigurationForAreaCode(String areaCode) {
        return usingStandardCarbonAwareConfiguration().andCarbonIntensityApiUrl(carbonIntensityApiBaseUrl).andAreaCode("BE");
    }

    protected Instant toEndOfNextDay(ZonedDateTime dateTime) {
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
                        forecast
                )
        );
    }
}
