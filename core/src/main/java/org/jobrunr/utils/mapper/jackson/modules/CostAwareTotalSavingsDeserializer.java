package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.jobrunr.server.costaware.CostAwareTotalSavings;
import org.jobrunr.server.costaware.CostAwareTotalSavings.BackgroundJobServerSavings;

import java.io.IOException;
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
    public CostAwareTotalSavings deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
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

    private <K, V> HashMap<K, V> readMapField(JsonNode node, JavaType javaType, DeserializationContext deserializationContext) throws IOException {
        if (node == null || node.isNull()) {
            return new HashMap<>();
        }
        return deserializationContext.readTreeAsValue(node, javaType);
    }
}
