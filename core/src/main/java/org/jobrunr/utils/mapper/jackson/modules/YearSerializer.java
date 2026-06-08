package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.Year;

public class YearSerializer extends StdSerializer<Year> {

    protected YearSerializer() {
        super(Year.class);
    }

    @Override
    public void serialize(Year year, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(year.toString());
    }

    @Override
    public void serializeWithType(Year year, JsonGenerator jsonGenerator, SerializerProvider serializerProvider, TypeSerializer typeSerializer) throws IOException {
        WritableTypeId typeIdDef = typeSerializer.typeId(year, JsonToken.VALUE_STRING);

        typeSerializer.writeTypePrefix(jsonGenerator, typeIdDef);
        serialize(year, jsonGenerator, serializerProvider);
        typeSerializer.writeTypeSuffix(jsonGenerator, typeIdDef);
    }
}
