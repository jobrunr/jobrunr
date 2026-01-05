package org.jobrunr.utils.mapper;

import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.mapper.jackson3.Jackson3JsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;
import org.jobrunr.utils.reflection.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class JsonMapperFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonMapperFactory.class);

    private static Function<String, Boolean> isJsonMapperClassPresent = ReflectionUtils::classExists;

    public static JsonMapper createJsonMapper() {
        if (isJsonMapperClassPresent("kotlinx.serialization.json.Json")
                && isJsonMapperClassPresent("org.jobrunr.kotlin.utils.mapper.KotlinxSerializationJsonMapper")) {
            LOGGER.info("Creating JobRunr JsonMapper using Kotlin Serialization");
            return ReflectionUtils.newInstance("org.jobrunr.kotlin.utils.mapper.KotlinxSerializationJsonMapper");
        } else if (isJsonMapperClassPresent("tools.jackson.databind.ObjectMapper")) {
            LOGGER.info("Creating JobRunr JsonMapper using Jackson 3");
            return new Jackson3JsonMapper();
        } else if (isJsonMapperClassPresent("com.fasterxml.jackson.databind.ObjectMapper")) {
            LOGGER.info("Creating JobRunr JsonMapper using Jackson 2");
            return new JacksonJsonMapper();
        } else if (isJsonMapperClassPresent("com.google.gson.Gson")) {
            LOGGER.info("Creating JobRunr JsonMapper using Gson");
            return new GsonJsonMapper();
        } else if (isJsonMapperClassPresent("jakarta.json.bind.JsonbBuilder")) {
            LOGGER.info("Creating JobRunr JsonMapper using JSON-B");
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
