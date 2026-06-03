package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.jobrunr.server.costaware.CostAwareTotalSavings;
import org.jobrunr.server.costaware.CostAwareTotalSavings.BackgroundJobServerSavings;
import org.jobrunr.server.costaware.CostAwareTotalSavings.DailySavings;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.jobrunr.server.costaware.CostAwareTotalSavings.MonthlySavings;
import static org.jobrunr.server.costaware.CostAwareTotalSavings.YearlySavings;

public class CostAwareTotalSavingsDeserializer extends StdDeserializer<CostAwareTotalSavings> {
    private final ObjectMapper objectMapper;

    protected CostAwareTotalSavingsDeserializer() {
        super(CostAwareTotalSavings.class);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public CostAwareTotalSavings deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode node = deserializationContext.readTree(jsonParser);

        Map<UUID, BackgroundJobServerSavings> backgroundJobServerSavings = readMapField(
                node, "backgroundJobServerSavings", objectMapper, new TypeReference<HashMap<UUID, BackgroundJobServerSavings>>() {});
        Map<LocalDate, DailySavings> dailySavings = readMapField(
                node, "dailySavings", objectMapper, new TypeReference<HashMap<LocalDate, DailySavings>>() {});
        Map<YearMonth, MonthlySavings> monthlySavings = readMapField(
                node, "monthlySavings", objectMapper, new TypeReference<HashMap<YearMonth, MonthlySavings>>() {});
        Map<Year, YearlySavings> yearlySavings = readMapField(
                node, "yearlySavings", objectMapper, new TypeReference<HashMap<Year, YearlySavings>>() {});

        return new CostAwareTotalSavings(backgroundJobServerSavings, dailySavings, monthlySavings, yearlySavings);
    }

    private <K, V> Map<K, V> readMapField(JsonNode parentNode, String fieldName, ObjectMapper mapper, TypeReference<HashMap<K, V>> typeRef) throws IOException {
        JsonNode fieldNode = parentNode.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return new HashMap<>();
        }
        return mapper.readValue(mapper.treeAsTokens(fieldNode), typeRef);
    }
}
