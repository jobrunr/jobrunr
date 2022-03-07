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
    @Disabled("No regression introduced for JsonB coming from 4.0.0")
    void testSerializeAndDeserializeEnqueuedJobComingFrom4Dot0Dot0() {
    }

    @Test
    @Disabled("JsonB does not know type in actual list")
    void testCanSerializeCollections() {
    }
}