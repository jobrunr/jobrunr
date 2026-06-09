package org.jobrunr.utils.mapper.jackson3.modules;

import org.jobrunr.server.costaware.CostAwareTotalSavings.BackgroundJobServerSavings;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class BackgroundJobServerSavingsDeserializer extends StdDeserializer<BackgroundJobServerSavings> {
    protected BackgroundJobServerSavingsDeserializer() {
        super(BackgroundJobServerSavings.class);
    }

    @Override
    public BackgroundJobServerSavings deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws JacksonException {
        JsonNode node = deserializationContext.readTree(jsonParser);

        UUID serverId = UUID.fromString(node.get("serverId").asString());
        Instant createdAt = deserializationContext.readTreeAsValue(node.get("createdAt"), Instant.class);
        Instant removedAt = deserializationContext.readTreeAsValue(node.get("removedAt"), Instant.class);
        BigDecimal spotPrice = deserializationContext.readTreeAsValue(node.get("spotPrice"), BigDecimal.class);
        BigDecimal instancePrice = deserializationContext.readTreeAsValue(node.get("instancePrice"), BigDecimal.class);
        BigDecimal totalSavings = deserializationContext.readTreeAsValue(node.get("totalSavings"), BigDecimal.class);

        return new BackgroundJobServerSavings(serverId, createdAt, removedAt, totalSavings, spotPrice, instancePrice);
    }
}
