package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.YearMonth;

public class YearMonthSerializer extends StdSerializer<YearMonth> {

    protected YearMonthSerializer() {
        super(YearMonth.class);
    }

    @Override
    public void serialize(YearMonth yearMonth, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(yearMonth.toString());
    }

    @Override
    public void serializeWithType(YearMonth yearMonth, JsonGenerator jsonGenerator, SerializerProvider serializerProvider, TypeSerializer typeSerializer) throws IOException {
        WritableTypeId typeIdDef = typeSerializer.typeId(yearMonth, JsonToken.VALUE_STRING);

        typeSerializer.writeTypePrefix(jsonGenerator, typeIdDef);
        serialize(yearMonth, jsonGenerator, serializerProvider);
        typeSerializer.writeTypeSuffix(jsonGenerator, typeIdDef);
    }
}
