package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.jobrunr.server.costaware.CostAwareTotalSavings.BackgroundJobServerSavings;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class BackgroundJobServerSavingsDeserializer extends StdDeserializer<BackgroundJobServerSavings> {
    private final ObjectMapper objectMapper;

    protected BackgroundJobServerSavingsDeserializer() {
        super(BackgroundJobServerSavings.class);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public BackgroundJobServerSavings deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = deserializationContext.readTree(jsonParser);

        UUID serverId = UUID.fromString(node.get("serverId").asText());
        Instant createdAt = objectMapper.convertValue(node.get("createdAt"), Instant.class);
        Instant removedAt = objectMapper.convertValue(node.get("removedAt"), Instant.class);
        BigDecimal totalSavings = new BigDecimal(node.get("totalSavings").asText());

        return new BackgroundJobServerSavings(serverId, createdAt, removedAt, totalSavings);
    }
}
