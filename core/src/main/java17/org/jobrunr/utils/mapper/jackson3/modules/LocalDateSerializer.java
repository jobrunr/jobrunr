package org.jobrunr.utils.mapper.jackson3.modules;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

import java.time.LocalDate;

public class LocalDateSerializer extends StdSerializer<LocalDate> {

    protected LocalDateSerializer() {
        super(LocalDate.class);
    }

    @Override
    public void serialize(LocalDate localDate, JsonGenerator jsonGenerator, SerializationContext serializerProvider) throws JacksonException {
        jsonGenerator.writeString(localDate.toString());
    }

    @Override
    public void serializeWithType(LocalDate localDate, JsonGenerator jsonGenerator, SerializationContext serializationContext, TypeSerializer typeSerializer) throws JacksonException {
        WritableTypeId typeIdDef = typeSerializer.typeId(localDate, JsonToken.VALUE_STRING);

        typeSerializer.writeTypePrefix(jsonGenerator, serializationContext, typeIdDef);
        serialize(localDate, jsonGenerator, serializationContext);
        typeSerializer.writeTypeSuffix(jsonGenerator, serializationContext, typeIdDef);
    }

}
