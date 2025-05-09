package org.mockito;

import java.time.LocalDate;

public class LocalDateMocker {
    public static MockedStatic<LocalDate> mockLocalDate(LocalDate localDate) {
        MockedStatic<LocalDate> localDateMock = Mockito.mockStatic(LocalDate.class, Mockito.CALLS_REAL_METHODS);
        localDateMock.when(LocalDate::now).thenReturn(localDate);
        return localDateMock;
    }
}
