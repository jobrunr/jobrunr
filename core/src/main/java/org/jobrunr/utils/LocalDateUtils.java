package org.jobrunr.utils;

import java.time.LocalDate;
import java.time.ZoneId;

public class LocalDateUtils {

    public static LocalDate nowUsingSystemDefault() {
        return LocalDate.now(ZoneId.systemDefault());
    }

}
