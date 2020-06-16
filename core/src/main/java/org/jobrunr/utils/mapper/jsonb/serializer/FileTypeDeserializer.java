package org.jobrunr.utils.mapper.jsonb.serializer;

import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.stream.JsonParser;
import java.io.File;
import java.lang.reflect.Type;

public class FileTypeDeserializer implements JsonbDeserializer<File> {

    @Override
    public File deserialize(JsonParser jsonParser, DeserializationContext ctx, Type type) {
        final String value = jsonParser.getString();
        return new File(value);
    }
}
