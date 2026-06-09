package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.jobrunr.server.costaware.CostAwareTotalSavings.BackgroundJobServerSavings;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class BackgroundJobServerSavingsDeserializer extends StdDeserializer<BackgroundJobServerSavings> {
    protected BackgroundJobServerSavingsDeserializer() {
        super(BackgroundJobServerSavings.class);
    }

    @Override
    public BackgroundJobServerSavings deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = deserializationContext.readTree(jsonParser);

        UUID serverId = UUID.fromString(node.get("serverId").asText());
        Instant createdAt = deserializationContext.readTreeAsValue(node.get("createdAt"), Instant.class);
        Instant removedAt = deserializationContext.readTreeAsValue(node.get("removedAt"), Instant.class);
        BigDecimal spotPrice = deserializationContext.readTreeAsValue(node.get("spotPrice"), BigDecimal.class);
        BigDecimal instancePrice = deserializationContext.readTreeAsValue(node.get("instancePrice"), BigDecimal.class);
        BigDecimal totalSavings = deserializationContext.readTreeAsValue(node.get("totalSavings"), BigDecimal.class);

        return new BackgroundJobServerSavings(serverId, createdAt, removedAt, totalSavings, spotPrice, instancePrice);
    }
}
