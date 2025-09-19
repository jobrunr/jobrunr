package org.mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class InstantMocker {

    public static final Instant FIXED_INSTANT_RIGHT_BEFORE_THE_HOUR = Instant.parse("2022-12-13T13:59:58Z");
    public static final Instant FIXED_INSTANT_RIGHT_AT_THE_HOUR = Instant.parse("2022-12-13T14:00:00Z");
    public static final Instant FIXED_INSTANT_ONE_MINUTE_AFTER_THE_HOUR = Instant.parse("2022-12-13T14:00:55.500Z");
    public static final Instant FIXED_INSTANT_RIGHT_BEFORE_THE_MINUTE = Instant.parse("2022-12-14T08:35:55.500Z");
    public static final Instant FIXED_INSTANT_RIGHT_ON_THE_MINUTE = Instant.parse("2022-12-14T08:36:00.005Z");
    public static final Instant FIXED_INSTANT_RIGHT_AFTER_THE_HOUR = Instant.parse("2022-12-13T14:00:15Z");

    public static MockedStatic<Instant> mockTime(String instantAsString) {
        return mockTime(Instant.parse(instantAsString));
    }

    public static MockedStaticHolder mockTime(String instantAsString, String zoneId) {
        return mockTime(Instant.parse(instantAsString).atZone(ZoneId.of(zoneId)));
    }

    public static MockedStaticHolder mockTime(LocalDateTime localDateTime, ZoneId zoneId) {
        return mockTime(ZonedDateTime.of(localDateTime, zoneId));
    }

    public static MockedStaticHolder mockTime(ZonedDateTime zonedDateTime) {
        MockedStatic<ZonedDateTime> zonedDateTimeMock = Mockito.mockStatic(ZonedDateTime.class, Mockito.CALLS_REAL_METHODS);
        MockedStatic<Instant> instantMockedStatic = mockTime(zonedDateTime.toInstant());
        zonedDateTimeMock.when(() -> ZonedDateTime.now(ZoneId.systemDefault())).thenReturn(zonedDateTime);
        zonedDateTimeMock.when(() -> ZonedDateTime.now(Mockito.any(ZoneId.class))).thenReturn(zonedDateTime);
        zonedDateTimeMock.when(() -> ZonedDateTime.now(Mockito.any(Clock.class))).thenReturn(zonedDateTime);
        return new MockedStaticHolder(zonedDateTimeMock, instantMockedStatic);
    }

    public static MockedStatic<Instant> mockTime(Instant instant) {
        MockedStatic<Instant> instantMock = Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS);
        instantMock.when(() -> Instant.now()).thenReturn(instant);
        instantMock.when(() -> Instant.now(Mockito.any(Clock.class))).thenReturn(instant);
        return instantMock;
    }
}
