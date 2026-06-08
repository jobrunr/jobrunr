package org.jobrunr.utils.mapper.jackson3.modules;

import tools.jackson.databind.module.SimpleModule;

import java.time.Duration;
import java.time.LocalDate;

public class JobRunrTimeModule extends SimpleModule {

    public JobRunrTimeModule() {
        addSerializer(Duration.class, new DurationSerializer());
        addDeserializer(Duration.class, new DurationDeserializer());
        addSerializer(LocalDate.class, new LocalDateSerializer());
        addDeserializer(LocalDate.class, new LocalDateDeserializer());
    }
}
