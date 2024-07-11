package org.jobrunr.server.carbonaware;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.utils.JarUtils;
import org.jobrunr.utils.exceptions.Exceptions;
import org.jobrunr.utils.mapper.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

public class CarbonIntensityApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonIntensityApiClient.class);

    private final CarbonAwareConfigurationReader carbonAwareConfiguration;
    private final JsonMapper jsonMapper;

    public CarbonIntensityApiClient(CarbonAwareConfigurationReader carbonAwareConfiguration, JsonMapper jsonMapper) {
        this.carbonAwareConfiguration = carbonAwareConfiguration;
        this.jsonMapper = jsonMapper;
    }

    public CarbonIntensityForecast fetchLatestCarbonIntensityForecast() {
        try {
            String carbonIntensityForecastAsString = fetchLatestCarbonIntensityForecastAsStringWithRetries();
            return jsonMapper.deserialize(carbonIntensityForecastAsString, CarbonIntensityForecast.class);
        } catch (Exception e) {
            LOGGER.error("Error processing energy prices for area code '{}'", carbonAwareConfiguration.getAreaCode(), e);
            return new CarbonIntensityForecast();
        }
    }

    private String fetchLatestCarbonIntensityForecastAsStringWithRetries() {
        return Exceptions.retryOnException(this::fetchLatestCarbonIntensityForecastAsString, 3, 1000);
    }

    private String fetchLatestCarbonIntensityForecastAsString() {
        HttpURLConnection connection = null;
        try {
            connection = createHttpConnection(carbonAwareConfiguration.getCarbonIntensityForecastApiFullPathUrl());
            return readResponse(connection);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        connection.setConnectTimeout((int) carbonAwareConfiguration.getApiClientConnectTimeout().toMillis());
        connection.setReadTimeout((int) carbonAwareConfiguration.getApiClientReadTimeout().toMillis());
    }

    private String readResponse(HttpURLConnection con) throws IOException {
        if (con.getResponseCode() > 299) {
            throw new CarbonIntensityApiClientException(con.getResponseCode(), readErrorStream(con));
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            StringBuilder content = new StringBuilder();
            reader.lines().forEach(content::append);
            return content.toString();
        }
    }

    private String readErrorStream(HttpURLConnection connection) throws IOException {
        if (connection.getErrorStream() != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
        return "No error details available";
    }
}
