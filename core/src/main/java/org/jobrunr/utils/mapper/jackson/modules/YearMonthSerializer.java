package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;

public class YearMonthSerializer extends StdSerializer<YearMonth> {

    protected YearMonthSerializer() {
        super(YearMonth.class);
    }

    @Override
    public void serialize(YearMonth yearMonth, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(yearMonth.toString());
    }
}
