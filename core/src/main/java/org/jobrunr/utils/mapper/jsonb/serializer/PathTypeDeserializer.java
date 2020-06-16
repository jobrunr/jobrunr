package org.jobrunr.utils.mapper.jsonb.serializer;

import javax.json.bind.serializer.DeserializationContext;
import javax.json.bind.serializer.JsonbDeserializer;
import javax.json.stream.JsonParser;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathTypeDeserializer implements JsonbDeserializer<Path> {

    @Override
    public Path deserialize(JsonParser jsonParser, DeserializationContext ctx, Type type) {
        final String value = jsonParser.getString();
        return Paths.get(value);
    }
}
