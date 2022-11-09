package org.jobrunr.utils.mapper;

import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class JsonbJsonMapperTest extends AbstractJsonMapperTest {

    @Override
    public JsonMapper newJsonMapper() {
        return new JsonbJsonMapper();
    }

    @Test
    @Disabled("I don't understand: custom deserializer is registered but get following exception: Unable to make field private final java.lang.String java.io.File.path accessible: module java.base does not \"opens java.io\" to unnamed module")
    void testSerializeAndDeserializeEnqueuedJobWithFileJobParameter() {
    }

    @Test
    @Disabled("No regression introduced for JsonB coming from 4.0.0")
    void testSerializeAndDeserializeEnqueuedJobComingFrom4Dot0Dot0() {
    }

    @Test
    @Disabled("JsonB does not know type in actual list")
    void testCanSerializeCollections() {
    }
}