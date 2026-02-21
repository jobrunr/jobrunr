package org.jobrunr.utils.mapper.jackson3.modules;

import org.jobrunr.jobs.states.FailedState;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.time.Instant;

public class FailedStateDeserializer extends StdDeserializer<FailedState> {

    protected FailedStateDeserializer() {
        super(FailedState.class);
    }

    @Override
    public FailedState deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws JacksonException {
        JsonNode node = ctxt.readTree(jsonParser);

        String message = getTextOrNull(node, "message");
        String exceptionType = getTextOrNull(node, "exceptionType");
        String exceptionMessage = getTextOrNull(node, "exceptionMessage");
        String exceptionCauseType = getTextOrNull(node, "exceptionCauseType");
        String exceptionCauseMessage = getTextOrNull(node, "exceptionCauseMessage");
        String stackTrace = getTextOrNull(node, "stackTrace");
        boolean doNotRetry = node.has("doNotRetry") && node.get("doNotRetry").asBoolean();
        Instant createdAt = node.has("createdAt") ? Instant.parse(node.get("createdAt").asString()) : Instant.now();

        return new FailedState(message, exceptionType, exceptionMessage, exceptionCauseType, exceptionCauseMessage, stackTrace, doNotRetry, createdAt);
    }

    private static String getTextOrNull(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull() ? node.get(fieldName).asString() : null;
    }
}
