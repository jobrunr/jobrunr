package org.jobrunr.server.carbonaware;

import static org.jobrunr.JobRunrAssertions.contentOfResource;

public class CarbonApiMockResponses {
    public static final String BELGIUM_2024_07_11 = contentOfResource("/carbonaware/api/belgium_2024-07-11.json");
    public static final String BELGIUM_PARTIAL_2024_07_11_FULL_2024_07_12 = contentOfResource("/carbonaware/api/belgium_partial_2024-07-11_full_2024-07-12.json");
    public static final String BELGIUM_PARTIAL_2024_07_12 = contentOfResource("/carbonaware/api/belgium_partial_2024-07-12.json");
    public static final String GERMANY_2024_07_11 = contentOfResource("/carbonaware/api/germany_2024-07-11.json");
    public static final String GERMANY_NO_DATA = contentOfResource("/carbonaware/api/germany_no_data.json");
    public static final String ITALY_2025_05_20_PT15M = contentOfResource("/carbonaware/api/italy_2025-05-20_PT15M.json");
    public static final String MISSING_FIELDS = contentOfResource("/carbonaware/api/missing_state_field.json");
    public static final String UNKNOWN_AREA = contentOfResource("/carbonaware/api/unknown_area.json");
}