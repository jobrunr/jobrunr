package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;

public class YearMonthDeserializer extends StdDeserializer<YearMonth> {

    protected YearMonthDeserializer() {
        super(YearMonth.class);
    }

    @Override
    public YearMonth deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        return YearMonth.parse(jsonParser.getText());
    }
}
