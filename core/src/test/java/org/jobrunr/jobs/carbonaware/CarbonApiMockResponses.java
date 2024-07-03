package org.jobrunr.jobs.carbonaware;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.jobrunr.JobRunrAssertions.contentOfResource;

public class CarbonApiMockResponses {
    public static final String BELGIUM_2024_03_12 = contentOfResource("/carbonaware/api/belgium_2024-03-12.json");
    public static final String GERMANY_2024_03_14 = contentOfResource("/carbonaware/api/germany_2024-03-14.json");
    public static final String GERMANY_2500_01_01 = contentOfResource("/carbonaware/api/germany_2500-01-01.json");
    public static final String BELGIUM_2024_03_14 = contentOfResource("/carbonaware/api/belgium_2024-03-14.json");
    public static final String GERMANY_NO_DATA = contentOfResource("/carbonaware/api/germany_no_data.json");
    public static final String MISSING_STATE_FIELD = contentOfResource("/carbonaware/api/missing_state_field.json");
    public static final String EXTRA_FIELD = contentOfResource("/carbonaware/api/extra_field.json");
    public static final String UNKNOWN_AREA = contentOfResource("/carbonaware/api/unknown_area.json");
    public static final String INVALID_JSON = contentOfResource("/carbonaware/api/invalid_json.json");

    public static final String BELGIUM_TOMORROW = contentOfResource("/carbonaware/api/belgium_tomorrow.json").replace("%s", LocalDate.now(ZoneId.of("UTC")).plusDays(1).toString());
}