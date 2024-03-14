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
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CarbonAwareApiClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonAwareApiClient.class);

    private final JsonMapper jsonMapper;

    public CarbonAwareApiClient(JsonMapper jsonMapper) {
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
        URL apiUrl = new URL(getJobRunrApiDayAheadEnergyPricesUrl(area));
        HttpURLConnection con = (HttpURLConnection) apiUrl.openConnection();
        con.setRequestProperty("User-Agent", "JobRunr " + JarUtils.getVersion(JobRunr.class));
        con.setRequestMethod("GET");
        con.setConnectTimeout(3000);
        con.setReadTimeout(3000);

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

    private String getJobRunrApiDayAheadEnergyPricesUrl(Optional<String> area) {
//        return "https://api.jobrunr.io/api/carbon-intensity/v1/day-ahead-energy-prices" + area.map(a -> "?area=" + a).orElse("");
        return CarbonAwareConfiguration.getCarbonAwareApiBaseUrl() + "/v1/day-ahead-energy-prices" + area.map(a -> "?area=" + a).orElse("");
    }
}
