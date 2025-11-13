package org.jobrunr.utils.mapper.jackson.modules;

import tools.jackson.databind.module.SimpleModule;

import java.time.Duration;

public class JobRunrJackson3TimeModule extends SimpleModule {

    public JobRunrJackson3TimeModule() {
        addSerializer(Duration.class, new DurationJackson3Serializer());
        addDeserializer(Duration.class, new DurationJackson3Deserializer());
    }
}
