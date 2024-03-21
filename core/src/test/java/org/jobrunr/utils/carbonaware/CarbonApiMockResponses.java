package org.jobrunr.utils.carbonaware;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class CarbonApiMockResponses {

    private static String getJsonResponse(String fileName) {
        try {
            // Adjusted to use the class loader and get the resource as a stream
            InputStream is = DayAheadEnergyPrices.class.getClassLoader().getResourceAsStream(fileName);
            if (is == null) {
                throw new FileNotFoundException("Resource not found: " + fileName);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static final String BELGIUM_2024_03_12 = getJsonResponse("org/jobrunr/utils/carbonaware/apiresponses/belgium_2024-03-12.json");

    public static final String GERMANY_2024_03_14 = "{\n" +
            "  \"isErrorResponse\": false,\n" +
            "  \"area\": \"DE\",\n" +
            "  \"state\": null,\n" +
            "  \"hoursAvailable\": 12,\n" +
            "  \"unit\": \"EUR/MWH\",\n" +
            "  \"timezone\": \"Europe/Berlin\",\n" +
            "  \"hourlyEnergyPrices\": [\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-14T12:00:00Z\",\n" +
            "      \"price\": 49.16,\n" +
            "      \"rank\": 1\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-14T22:00:00Z\",\n" +
            "      \"price\": 51.66,\n" +
            "      \"rank\": 2\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-14T11:00:00Z\",\n" +
            "      \"price\": 52.83,\n" +
            "      \"rank\": 3\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-14T13:00:00Z\",\n" +
            "      \"price\": 52.84,\n" +
            "      \"rank\": 4\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-14T14:00:00Z\",\n" +
            "      \"price\": 58.91,\n" +
            "      \"rank\": 5\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-14T21:00:00Z\",\n" +
            "      \"price\": 59.62,\n" +
            "      \"rank\": 6\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-14T20:00:00Z\",\n" +
            "      \"price\": 62.63,\n" +
            "      \"rank\": 7\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-14T15:00:00Z\",\n" +
            "      \"price\": 73.21,\n" +
            "      \"rank\": 8\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-14T19:00:00Z\",\n" +
            "      \"price\": 74.09,\n" +
            "      \"rank\": 9\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-14T18:00:00Z\",\n" +
            "      \"price\": 91.04,\n" +
            "      \"rank\": 10\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-14T16:00:00Z\",\n" +
            "      \"price\": 91.04,\n" +
            "      \"rank\": 11\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-14T17:00:00Z\",\n" +
            "      \"price\": 114.77,\n" +
            "      \"rank\": 12\n" +
            "    }\n" +
            "  ]\n" +
            "}";


    public static final String GERMANY_SATURDAY_2024_03_16 = "{\n" +
            "  \"isErrorResponse\": false,\n" +
            "  \"area\": \"DE\",\n" +
            "  \"state\": null,\n" +
            "  \"hoursAvailable\": 12,\n" +
            "  \"unit\": \"EUR/MWH\",\n" +
            "  \"timezone\": \"Europe/Berlin\",\n" +
            "  \"hourlyEnergyPrices\": [\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-16T12:00:00Z\",\n" +
            "      \"price\": 49.16,\n" +
            "      \"rank\": 1\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-16T22:00:00Z\",\n" +
            "      \"price\": 51.66,\n" +
            "      \"rank\": 2\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-16T11:00:00Z\",\n" +
            "      \"price\": 52.83,\n" +
            "      \"rank\": 3\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-16T13:00:00Z\",\n" +
            "      \"price\": 52.84,\n" +
            "      \"rank\": 4\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-16T14:00:00Z\",\n" +
            "      \"price\": 58.91,\n" +
            "      \"rank\": 5\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-16T21:00:00Z\",\n" +
            "      \"price\": 59.62,\n" +
            "      \"rank\": 6\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-16T20:00:00Z\",\n" +
            "      \"price\": 62.63,\n" +
            "      \"rank\": 7\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-16T15:00:00Z\",\n" +
            "      \"price\": 73.21,\n" +
            "      \"rank\": 8\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-16T19:00:00Z\",\n" +
            "      \"price\": 74.09,\n" +
            "      \"rank\": 9\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-16T18:00:00Z\",\n" +
            "      \"price\": 91.04,\n" +
            "      \"rank\": 10\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-16T16:00:00Z\",\n" +
            "      \"price\": 91.04,\n" +
            "      \"rank\": 11\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-16T17:00:00Z\",\n" +
            "      \"price\": 114.77,\n" +
            "      \"rank\": 12\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    public static final String GERMANY_SUNDAY_2024_03_17 = "{\n" +
            "  \"isErrorResponse\": false,\n" +
            "  \"area\": \"DE\",\n" +
            "  \"state\": null,\n" +
            "  \"hoursAvailable\": 12,\n" +
            "  \"unit\": \"EUR/MWH\",\n" +
            "  \"timezone\": \"Europe/Berlin\",\n" +
            "  \"hourlyEnergyPrices\": [\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-17T12:00:00Z\",\n" +
            "      \"price\": 49.16,\n" +
            "      \"rank\": 1\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-17T22:00:00Z\",\n" +
            "      \"price\": 51.66,\n" +
            "      \"rank\": 2\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-17T11:00:00Z\",\n" +
            "      \"price\": 52.83,\n" +
            "      \"rank\": 3\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-17T13:00:00Z\",\n" +
            "      \"price\": 52.84,\n" +
            "      \"rank\": 4\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-17T14:00:00Z\",\n" +
            "      \"price\": 58.91,\n" +
            "      \"rank\": 5\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-17T21:00:00Z\",\n" +
            "      \"price\": 59.62,\n" +
            "      \"rank\": 6\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-17T20:00:00Z\",\n" +
            "      \"price\": 62.63,\n" +
            "      \"rank\": 7\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-17T15:00:00Z\",\n" +
            "      \"price\": 73.21,\n" +
            "      \"rank\": 8\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-17T19:00:00Z\",\n" +
            "      \"price\": 74.09,\n" +
            "      \"rank\": 9\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-17T18:00:00Z\",\n" +
            "      \"price\": 91.04,\n" +
            "      \"rank\": 10\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-17T16:00:00Z\",\n" +
            "      \"price\": 91.04,\n" +
            "      \"rank\": 11\n" +
            "    },\n" +
            "    {\n" +
            "      \"dateTime\": \"2024-03-17T17:00:00Z\",\n" +
            "      \"price\": 114.77,\n" +
            "      \"rank\": 12\n" +
            "    }\n" +
            "  ]\n" +
            "}";
}