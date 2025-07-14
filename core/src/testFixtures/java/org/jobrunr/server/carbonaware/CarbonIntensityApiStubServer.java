package org.jobrunr.server.carbonaware;

import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.dashboard.server.HttpExchangeHandler;
import org.jobrunr.dashboard.server.WebServer;
import org.jobrunr.dashboard.server.http.ContentType;
import org.mockito.internal.util.reflection.Whitebox;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.time.ZoneId.systemDefault;
import static org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfigurationReader.getCarbonIntensityForecastApiRootUrl;

public class CarbonIntensityApiStubServer {

    private WebServer webServer;

    private int port = 10000;
    private List<IntensityMoment> intensityMoments = new ArrayList<>();
    public static final int CARBON_LOW_INTENSITY = 1;
    public static final int CARBON_HIGH_INTENSITY = 5;

    public CarbonIntensityApiStubServer andCarbonAwareJobProcessingConfig(CarbonAwareJobProcessingConfiguration carbonConfig) {
        Whitebox.setInternalState(carbonConfig, "carbonIntensityApiUrl", getCarbonIntensityForecastApiRootUrl("http://localhost:" + port));
        return this;
    }

    private static class IntensityMoment {
        private final Instant start;
        private final Instant end;
        private int rank;

        private IntensityMoment(int i) {
            this(Instant.now().plus(i, ChronoUnit.HOURS), CARBON_HIGH_INTENSITY);
        }

        private int getHour() {
            return start.atZone(systemDefault()).getHour();
        }

        private IntensityMoment(Instant start, int rank) {
            this.start = start;
            this.end = start.plus(1, ChronoUnit.HOURS);
            this.rank = rank;
        }

        @Override
        public String toString() {
            var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00:00'Z'");
            return String.format("    {\n" +
                    "      \"periodStartAt\": \"%s\",\n" +
                    "      \"periodEndAt\": \"%s\",\n" +
                    "      \"rank\": %s\n" +
                    "    }", formatter.format(start.atZone(ZoneOffset.UTC)), formatter.format(end.atZone(ZoneOffset.UTC)), rank);
        }
    }

    public static void main(String[] args) {
        var server = new CarbonIntensityApiStubServer()
                .andBestIntensityMomentTodayAt(11);
        System.out.println("Carbon Intensity Stub Server started on port " + server.port + ".");
        server.start();
    }

    public CarbonIntensityApiStubServer() {
        for (int i = 0; i < 24; i++) {
            intensityMoments.add(new IntensityMoment(i));
        }
    }

    public CarbonIntensityApiStubServer andPort(int port) {
        this.port = port;
        return this;
    }

    public CarbonIntensityApiStubServer andBestIntensityMomentTodayAt(int hourInLocalTime) {
        intensityMoments = new ArrayList<>();

        for (int i = 0; i < 24; i++) {
            IntensityMoment intensityMoment = new IntensityMoment(i);
            intensityMoment.rank = intensityMoment.getHour() == hourInLocalTime ? CARBON_LOW_INTENSITY : CARBON_HIGH_INTENSITY;
            intensityMoments.add(intensityMoment);
        }
        return this;
    }

    private String getIntensityForecastArrayJson() {
        return "[" + intensityMoments.stream()
                .map(IntensityMoment::toString)
                .collect(Collectors.joining(", ")) + "]";
    }

    private String getIntensityJson() {
        var now = Instant.now();
        var nextForecastAvailableAt = now.plus(24, ChronoUnit.HOURS);
        return String.format("{\n" +
                "  \"apiResponse\": {\n" +
                "    \"code\": \"OK\",\n" +
                "    \"message\": \"DataProvider ENTSO-E and area Belgium has 24 forecasts.\"\n" +
                "  },\n" +
                "  \"dataProvider\": \"ENTSO-E\",\n" +
                "  \"dataIdentifier\": \"10YBE----------2\",\n" +
                "  \"displayName\": \"Belgium\",\n" +
                "  \"timezone\": \"Europe/Brussels\",\n" +
                "  \"nextForecastAvailableAt\": \"%s\",\n" +
                "  \"forecastInterval\": \"PT1H\",\n" +
                "  \"intensityForecast\": %s\n" +
                "}", nextForecastAvailableAt, getIntensityForecastArrayJson());
    }

    public CarbonIntensityApiStubServer start() {
        webServer = new WebServer(port);
        webServer.createContext(new HttpExchangeHandler() {
            @Override
            public String getContextPath() {
                return CarbonAwareJobProcessingConfigurationReader.getCarbonIntensityForecastApiPath();
            }

            @Override
            public void close() {
            }

            @Override
            public void handle(HttpExchange httpExchange) {
                httpExchange.getResponseHeaders().add(ContentType._HEADER_NAME, ContentType.APPLICATION_JSON);
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                try (var outputStream = httpExchange.getResponseBody()) {
                    httpExchange.sendResponseHeaders(200, 0);
                    outputStream.write(getIntensityJson().getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
        webServer.start();
        return this;
    }

    public void stop() {
        if (webServer == null) return;

        webServer.stop();
        webServer = null;
    }

}
