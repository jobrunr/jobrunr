package org.jobrunr.utils.mapper.jsonb.serializer;

import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

import java.nio.file.Path;

public class PathTypeSerializer implements JsonbSerializer<Path> {

    @Override
    public void serialize(Path path, JsonGenerator jsonGenerator, SerializationContext serializationContext) {
        jsonGenerator.write(path.toString());
    }
}
