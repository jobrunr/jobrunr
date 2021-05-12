package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.jobrunr.jobs.JobParameter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.jobrunr.utils.mapper.JsonMapperUtils.Json.FIELD_ACTUAL_CLASS_NAME;
import static org.jobrunr.utils.mapper.JsonMapperUtils.Json.FIELD_CLASS_NAME;
import static org.jobrunr.utils.mapper.JsonMapperUtils.getActualClassName;
import static org.jobrunr.utils.reflection.ReflectionUtils.toClass;

public class JobParameterDeserializer extends StdDeserializer<JobParameter> {

    protected JobParameterDeserializer() {
        super(JobParameter.class);
    }

    @Override
    public JobParameter deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        final String methodClassName = node.get(FIELD_CLASS_NAME).asText();
        final String actualClassName = node.has(FIELD_ACTUAL_CLASS_NAME) ? node.get(FIELD_ACTUAL_CLASS_NAME).asText() : null;
        final JsonNode objectJsonNode = node.get("object");
        if (Path.class.getName().equals(methodClassName)) { // see https://github.com/FasterXML/jackson-databind/issues/2013
            return new JobParameter(methodClassName, Paths.get(objectJsonNode.asText().replace("file:", "")));
        } else {
            final Object object = jsonParser.getCodec().treeToValue(objectJsonNode, toClass(getActualClassName(methodClassName, actualClassName)));
            return new JobParameter(methodClassName, object);
        }
    }
}
