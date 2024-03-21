package org.mockito;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class DatetimeMocker {
    public static MockedStatic<ZonedDateTime> mockZonedDateTime(ZonedDateTime zonedDateTime, String zoneId) {
        MockedStatic<ZonedDateTime> zonedDateTimeMock = Mockito.mockStatic(ZonedDateTime.class, Mockito.CALLS_REAL_METHODS);
        zonedDateTimeMock.when(() -> ZonedDateTime.now(ZoneId.of(zoneId))).thenReturn(zonedDateTime);
        return zonedDateTimeMock;
    }
}
