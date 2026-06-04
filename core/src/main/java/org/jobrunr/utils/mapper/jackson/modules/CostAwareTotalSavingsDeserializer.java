package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
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

    protected CostAwareTotalSavingsDeserializer() {
        super(CostAwareTotalSavings.class);
    }

    @Override
    public CostAwareTotalSavings deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = deserializationContext.readTree(jsonParser);

        TypeFactory typeFactory = deserializationContext.getTypeFactory();

        Map<UUID, BackgroundJobServerSavings> backgroundJobServerSavings = readMapField(
                node.get("backgroundJobServerSavings"), typeFactory.constructMapType(HashMap.class, UUID.class, BackgroundJobServerSavings.class), deserializationContext
        );
        Map<LocalDate, DailySavings> dailySavings = readMapField(
                node.get("dailySavings"), typeFactory.constructMapType(HashMap.class, LocalDate.class, DailySavings.class), deserializationContext
        );
        Map<YearMonth, MonthlySavings> monthlySavings = readMapField(
                node.get("monthlySavings"), typeFactory.constructMapType(HashMap.class, YearMonth.class, MonthlySavings.class), deserializationContext
        );
        Map<Year, YearlySavings> yearlySavings = readMapField(
                node.get("yearlySavings"), typeFactory.constructMapType(HashMap.class, Year.class, YearlySavings.class), deserializationContext
        );

        return new CostAwareTotalSavings(backgroundJobServerSavings, dailySavings, monthlySavings, yearlySavings);
    }

    private <K, V> Map<K, V> readMapField(JsonNode node, JavaType javaType, DeserializationContext deserializationContext) throws IOException {
        if (node == null || node.isNull()) {
            return new HashMap<>();
        }
        return deserializationContext.readTreeAsValue(node, javaType);
    }
}
