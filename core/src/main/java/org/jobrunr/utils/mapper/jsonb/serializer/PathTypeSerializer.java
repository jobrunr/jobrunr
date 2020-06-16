package org.jobrunr.utils.mapper.jsonb.serializer;

import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;
import java.nio.file.Path;

public class PathTypeSerializer implements JsonbSerializer<Path> {

    @Override
    public void serialize(Path path, JsonGenerator jsonGenerator, SerializationContext serializationContext) {
        jsonGenerator.write(path.toString());
    }
}
