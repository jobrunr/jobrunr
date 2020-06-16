package org.jobrunr.utils.mapper.jsonb.serializer;

import javax.json.bind.serializer.JsonbSerializer;
import javax.json.bind.serializer.SerializationContext;
import javax.json.stream.JsonGenerator;
import java.io.File;

public class FileTypeSerializer implements JsonbSerializer<File> {

    @Override
    public void serialize(File file, JsonGenerator jsonGenerator, SerializationContext serializationContext) {
        jsonGenerator.write(file.toString());
    }
}
