package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.databind.module.SimpleModule;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;

public class JobRunrTimeModule extends SimpleModule {

    public JobRunrTimeModule() {
        addSerializer(Instant.class, new InstantSerializer());
        addDeserializer(Instant.class, new InstantDeserializer());
        addSerializer(Duration.class, new DurationSerializer());
        addDeserializer(Duration.class, new DurationDeserializer());
        addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
        addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer());
        addSerializer(LocalDate.class, new LocalDateSerializer());
        addDeserializer(LocalDate.class, new LocalDateDeserializer());
        addSerializer(LocalTime.class, new LocalTimeSerializer());
        addDeserializer(LocalTime.class, new LocalTimeDeserializer());
        addSerializer(YearMonth.class, new YearMonthSerializer());
        addDeserializer(YearMonth.class, new YearMonthDeserializer());
        addSerializer(Year.class, new YearSerializer());
        addDeserializer(Year.class, new YearDeserializer());

        addKeyDeserializer(LocalDate.class, new LocalDateKeyDeserializer());
        addKeyDeserializer(YearMonth.class, new YearMonthKeyDeserializer());
        addKeyDeserializer(Year.class, new YearKeyDeserializer());
    }
}
