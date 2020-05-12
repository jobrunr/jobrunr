package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.jobrunr.jobs.JobParameter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.jobrunr.utils.reflection.ReflectionUtils.toClass;

public class JobParameterDeserializer extends StdDeserializer<JobParameter> {

    protected JobParameterDeserializer() {
        super(JobParameter.class);
    }

    @Override
    public JobParameter deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        final String className = node.get("className").asText();
        final JsonNode objectJsonNode = node.get("object");
        if (Path.class.getName().equals(className)) { // see https://github.com/FasterXML/jackson-databind/issues/2013
            return new JobParameter(className, Paths.get(objectJsonNode.asText().replace("file:", "")));
        } else {
            final Object object = jsonParser.getCodec().treeToValue(objectJsonNode, toClass(className));
            return new JobParameter(className, object);
        }
    }

}
