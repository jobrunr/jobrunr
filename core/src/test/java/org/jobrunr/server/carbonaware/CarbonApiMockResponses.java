package org.jobrunr.server.carbonaware;

import static org.jobrunr.JobRunrAssertions.contentOfResource;

public class CarbonApiMockResponses {
    public static final String BELGIUM_2024_07_11 = contentOfResource("/carbonaware/api/belgium_2024-07-11.json");
    public static final String GERMANY_2024_07_11 = contentOfResource("/carbonaware/api/germany_2024-07-11.json");
    public static final String GERMANY_NO_DATA = contentOfResource("/carbonaware/api/germany_no_data.json");
    public static final String MISSING_STATE_FIELD = contentOfResource("/carbonaware/api/missing_state_field.json");
    public static final String EXTRA_FIELD = contentOfResource("/carbonaware/api/extra_field.json");
    public static final String UNKNOWN_AREA = contentOfResource("/carbonaware/api/unknown_area.json");
}