package org.jobrunr.utils.carbonaware;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.utils.JarUtils;
import org.jobrunr.utils.annotations.Retry;
import org.jobrunr.utils.mapper.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CarbonAwareApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonAwareApiClient.class);

    private final String carbonAwareApiUrl;
    private final Duration apiClientConnectTimeout;
    private final Duration apiClientReadTimeout;
    private final JsonMapper jsonMapper;

    public CarbonAwareApiClient(String carbonAwareApiUrl, Duration apiClientConnectTimeout, Duration apiClientReadTimeout, JsonMapper jsonMapper) {
        this.carbonAwareApiUrl = carbonAwareApiUrl;
        this.apiClientConnectTimeout = apiClientConnectTimeout;
        this.apiClientReadTimeout = apiClientReadTimeout;
        this.jsonMapper = jsonMapper;
    }

    public DayAheadEnergyPrices fetchLatestDayAheadEnergyPrices(Optional<String> areaCode) {
        try {
            String dayAheadEnergyPricesAsString = fetchLatestDayAheadEnergyPricesAsString(areaCode);
            return jsonMapper.deserialize(dayAheadEnergyPricesAsString, DayAheadEnergyPrices.class);
        } catch (IOException e) {
            String errorMessage = String.format("Network error fetching energy prices for area code '%s': %s", areaCode.orElse("unknown"), e.getMessage());
            LOGGER.error(errorMessage, e);
            return createErrorResponse(errorMessage);
        } catch (Exception e) {
            String errorMessage = String.format("Error processing energy prices for area code '%s': %s", areaCode.orElse("unknown"), e.getMessage());
            LOGGER.error(errorMessage, e);
            return createErrorResponse(errorMessage);
        }
    }

    @Retry(maxAttempts = 3, delayMs = 100)
    private String fetchLatestDayAheadEnergyPricesAsString(Optional<String> areaCode) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = createHttpConnection(getCarbonAwareApiDayAheadEnergyPricesUrl(areaCode));
            return readResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private HttpURLConnection createHttpConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        configureConnection(connection);
        return connection;
    }

    private void configureConnection(HttpURLConnection connection) throws IOException {
        connection.setRequestProperty("User-Agent", "JobRunr " + JarUtils.getVersion(JobRunr.class));
        connection.setRequestMethod("GET");
        connection.setConnectTimeout((int) apiClientConnectTimeout.toMillis());
        connection.setReadTimeout((int) apiClientReadTimeout.toMillis());
    }

    private String readResponse(HttpURLConnection con) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            reader.lines().forEach(content::append);
            return content.toString();
        } catch (IOException e) {
            if (con.getResponseCode() != 200) {
                throw new IOException("Error from server: " + readErrorStream(con), e);
            }
            throw e;
        }
    }

    protected URL getCarbonAwareApiDayAheadEnergyPricesUrl(Optional<String> areaCode) throws MalformedURLException {
        return new URL(carbonAwareApiUrl + areaCode.map(a -> "&areaCode=" + a).orElse(""));
    }

    private String readErrorStream(HttpURLConnection connection) throws IOException {
        if (connection.getErrorStream() != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
        return "No error details available";
    }

    private DayAheadEnergyPrices createErrorResponse(String errorMessage) {
        DayAheadEnergyPrices errorResponse = new DayAheadEnergyPrices();
        errorResponse.setErrorMessage(errorMessage);
        return errorResponse;
    }
}
