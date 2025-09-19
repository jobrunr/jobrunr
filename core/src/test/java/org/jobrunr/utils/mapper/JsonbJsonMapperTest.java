package org.jobrunr.utils.mapper;

import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class JsonbJsonMapperTest extends AbstractJsonMapperTest {

    @Override
    public JsonMapper newJsonMapper() {
        return new JsonbJsonMapper();
    }

    @Override
    @Test
    @Disabled("I don't understand: custom deserializer is registered but get following exception: Unable to make field private final java.lang.String java.io.File.path accessible: module java.base does not \"opens java.io\" to unnamed module")
    void testSerializeAndDeserializeEnqueuedJobWithFileJobParameter() {
    }

    @Override
    @Test
    @Disabled("No regression introduced for JsonB coming from 4.0.0")
    protected void testSerializeAndDeserializeEnqueuedJobComingFrom4Dot0Dot0() {
    }

    @Override
    @Test
    @Disabled("JsonB does not know type in actual list")
    void testCanSerializeCollections() {
    }

    @Override
    @Test
    @Disabled("https://github.com/eclipse-ee4j/yasson/issues/641")
    void testCanSerializeEnums() {
    }

    @Override
    @Test
    @Disabled("https://github.com/eclipse-ee4j/yasson/issues/641")
    void testCanSerializeAndDeserializeWithAllFieldsNotNull() {
    }
}