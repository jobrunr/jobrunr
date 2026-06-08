package org.jobrunr.utils.mapper.jackson3.modules;

import org.jobrunr.server.costaware.CostAwareTotalSavings;
import org.jobrunr.server.costaware.CostAwareTotalSavings.BackgroundJobServerSavings;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.type.MapType;
import tools.jackson.databind.type.TypeFactory;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.UUID;

import static org.jobrunr.server.costaware.CostAwareTotalSavings.Savings;

public class CostAwareTotalSavingsDeserializer extends StdDeserializer<CostAwareTotalSavings> {

    protected CostAwareTotalSavingsDeserializer() {
        super(CostAwareTotalSavings.class);
    }

    @Override
    public CostAwareTotalSavings deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws JacksonException {
        JsonNode node = deserializationContext.readTree(jsonParser);

        TypeFactory typeFactory = deserializationContext.getTypeFactory();

        HashMap<UUID, BackgroundJobServerSavings> backgroundJobServerSavings = readMapField(
                node.get("backgroundJobServerSavings"), typeFactory.constructMapType(HashMap.class, UUID.class, BackgroundJobServerSavings.class), deserializationContext
        );
        HashMap<LocalDate, Savings> dailySavings = readMapField(
                node.get("dailySavings"), typeFactory.constructMapType(HashMap.class, LocalDate.class, Savings.class), deserializationContext
        );
        HashMap<YearMonth, Savings> monthlySavings = readMapField(
                node.get("monthlySavings"), typeFactory.constructMapType(HashMap.class, YearMonth.class, Savings.class), deserializationContext
        );
        HashMap<Year, Savings> yearlySavings = readMapField(
                node.get("yearlySavings"), typeFactory.constructMapType(HashMap.class, Year.class, Savings.class), deserializationContext
        );

        return new CostAwareTotalSavings(backgroundJobServerSavings, dailySavings, monthlySavings, yearlySavings);
    }

    private <K, V> HashMap<K, V> readMapField(JsonNode node, MapType javaType, DeserializationContext deserializationContext) {
        if (node == null || node.isNull()) {
            return new HashMap<>();
        }
        return deserializationContext.readTreeAsValue(node, javaType);
    }
}
