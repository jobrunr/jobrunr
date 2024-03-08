package org.jobrunr.utils.carbonaware;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.utils.JarUtils;
import org.jobrunr.utils.annotations.VisibleFor;
import org.jobrunr.utils.mapper.JsonMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class CarbonAwareAPIClient {

    private final JsonMapper jsonMapper;

    public CarbonAwareAPIClient(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public DayAheadEnergyPrices getDayAheadEnergyPrices(Optional<String> area) {
        return fetchLatestDayAheadEnergyPrices(area);
    }

    DayAheadEnergyPrices fetchLatestDayAheadEnergyPrices(Optional<String> area) {
        try {
            String dayAheadEnergyPricesAsString = fetchLatestDayAheadEnergyPricesAsString(area);
            return jsonMapper.deserialize(dayAheadEnergyPricesAsString, DayAheadEnergyPrices.class);
        } catch (Exception e) {
            return DayAheadEnergyPrices.error(e.getMessage());
        }
    }

    @VisibleFor("testing")
    static String fetchLatestDayAheadEnergyPricesAsString(Optional<String> area) throws IOException {
        URL apiUrl = new URL(getJobRunrApiDayAheadEnergyPricesUrl(area));
        HttpURLConnection con = (HttpURLConnection) apiUrl.openConnection();
        con.setRequestProperty("User-Agent", "JobRunr " + JarUtils.getVersion(JobRunr.class));
        con.setRequestMethod("GET");
        con.setConnectTimeout(2000);
        con.setReadTimeout(2000);

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
        }
    }

    private static String getJobRunrApiDayAheadEnergyPricesUrl(Optional<String> area) {
        return "https://api.jobrunr.io/api/carbon-intensity/day-ahead-energy-prices" + area.map(a -> "?area=" + a).orElse("");
    }
}
