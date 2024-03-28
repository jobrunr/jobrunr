package org.jobrunr.utils.carbonaware;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.utils.JarUtils;
import org.jobrunr.utils.annotations.VisibleFor;
import org.jobrunr.utils.mapper.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Optional;

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

    public DayAheadEnergyPrices fetchLatestDayAheadEnergyPrices(Optional<String> area) {
        try {
            String dayAheadEnergyPricesAsString = fetchLatestDayAheadEnergyPricesAsString(area);
            return jsonMapper.deserialize(dayAheadEnergyPricesAsString, DayAheadEnergyPrices.class);
        } catch (Exception e) {
            LOGGER.error("Error fetching day ahead energy prices for area '{}'", area.orElse("unknown"), e);
            DayAheadEnergyPrices errorResponse = new DayAheadEnergyPrices();
            errorResponse.setIsErrorResponse(true);
            errorResponse.setErrorMessage(e.getMessage());
            return errorResponse;
        }
    }

    @VisibleFor("testing")
    String fetchLatestDayAheadEnergyPricesAsString(Optional<String> area) throws IOException {
        URL apiUrl = getJobRunrApiDayAheadEnergyPricesUrl(area);
        HttpURLConnection con = (HttpURLConnection) apiUrl.openConnection();
        con.setRequestProperty("User-Agent", "JobRunr " + JarUtils.getVersion(JobRunr.class));
        con.setRequestMethod("GET");
        con.setConnectTimeout((int)apiClientConnectTimeout.toMillis());
        con.setReadTimeout((int)apiClientReadTimeout.toMillis());

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            return content.toString();
        } catch (UnknownHostException e) {
            throw e;
        } catch (IOException e) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()))) {
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                throw new IOException(content.toString());
            }
            finally {
                con.disconnect();
            }
        }
    }

    private URL getJobRunrApiDayAheadEnergyPricesUrl(Optional<String> area) throws MalformedURLException {
        return new URL(carbonAwareApiUrl + area.map(a -> "?area=" + a).orElse(""));
    }
}
