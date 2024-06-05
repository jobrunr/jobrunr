package org.jobrunr.utils.carbonaware;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;

public class CarbonApiMockResponses {

    private static String getJsonResponse(String fileName) {
        try (InputStream is = DayAheadEnergyPrices.class.getClassLoader().getResourceAsStream(fileName)) {
            if (is == null) {
                throw new FileNotFoundException("Resource not found: " + fileName);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static final String BELGIUM_2024_03_12 = getJsonResponse("org/jobrunr/utils/carbonaware/apiresponses/belgium_2024-03-12.json");
    public static final String GERMANY_2024_03_14 = getJsonResponse("org/jobrunr/utils/carbonaware/apiresponses/germany_2024-03-14.json");
    public static final String GERMANY_2500_01_01 = getJsonResponse("org/jobrunr/utils/carbonaware/apiresponses/germany_2500-01-01.json");
    public static final String BELGIUM_2024_03_14 = getJsonResponse("org/jobrunr/utils/carbonaware/apiresponses/belgium_2024-03-14.json");
    public static final String GERMANY_NO_DATA = getJsonResponse("org/jobrunr/utils/carbonaware/apiresponses/germany_no_data.json");
    public static final String MISSING_UNIT_FIELD = getJsonResponse("org/jobrunr/utils/carbonaware/apiresponses/missing_unit_field.json");
    public static final String EXTRA_FIELD = getJsonResponse("org/jobrunr/utils/carbonaware/apiresponses/extra_field.json");
    public static final String INVALID_JSON = getJsonResponse("org/jobrunr/utils/carbonaware/apiresponses/invalid_json.json");

    public static final String BELGIUM_TOMORROW = getJsonResponse("org/jobrunr/utils/carbonaware/apiresponses/belgium_tomorrow.json").replace("%s", LocalDate.now(ZoneId.of("UTC")).plusDays(1).toString());
}