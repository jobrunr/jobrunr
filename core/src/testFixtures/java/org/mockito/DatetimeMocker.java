package org.mockito;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DatetimeMocker {
    public static MockedStatic<ZonedDateTime> mockZonedDateTime(ZonedDateTime zonedDateTime, String zoneId) {
        return mockZonedDateTime(zonedDateTime, ZoneId.of(zoneId));
    }

    public static MockedStatic<ZonedDateTime> mockZonedDateTime(ZonedDateTime zonedDateTime, ZoneId zoneId) {
        MockedStatic<ZonedDateTime> zonedDateTimeMock = Mockito.mockStatic(ZonedDateTime.class, Mockito.CALLS_REAL_METHODS);
        zonedDateTimeMock.when(() -> ZonedDateTime.now(zoneId)).thenReturn(zonedDateTime);
        return zonedDateTimeMock;
    }
}
