package org.jobrunr.utils.mapper;

import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class GsonJsonMapperTest extends AbstractJsonMapperTest {

    @Override
    public JsonMapper newJsonMapper() {
        return new GsonJsonMapper();
    }

    @Test
    @Disabled("No regression introduced for Gson coming from 4.0.0")
    void testSerializeAndDeserializeEnqueuedJobGithubIssue254ComingFrom4Dot0Dot0() {
    }

    @Test
    @Disabled("Gson does not know type in actual list")
    void testCanSerializeCollectionsGithubIssue282() {
    }
}
