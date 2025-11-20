package org.jobrunr.utils.mapper;

import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;
import org.jobrunr.utils.reflection.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class JsonMapperFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonMapperFactory.class);

    private static Function<String, Boolean> isJsonMapperClassPresent = ReflectionUtils::classExists;

    public static JsonMapper createJsonMapper() {
        if (isJsonMapperClassPresent("com.fasterxml.jackson.databind.ObjectMapper")) {
            LOGGER.debug("Creating JsonMapper using Jackson 2");
            return new JacksonJsonMapper();
        } else if (isJsonMapperClassPresent("com.google.gson.Gson")) {
            LOGGER.debug("Creating JsonMapper using Gson");
            return new GsonJsonMapper();
        } else if (isJsonMapperClassPresent("jakarta.json.bind.JsonbBuilder")) {
            LOGGER.debug("Creating JsonMapper using JSON-B");
            return new JsonbJsonMapper();
        }
        return null;
    }

    private static boolean isJsonMapperClassPresent(String className) {
        return isJsonMapperClassPresent.apply(className);
    }

    public static void setJsonMapperClassPresentFunction(Function<String, Boolean> isClassPresent) {
        JsonMapperFactory.isJsonMapperClassPresent = isClassPresent;
    }
}
