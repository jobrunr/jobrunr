package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.JobParameterNotDeserializableException;
import org.jobrunr.utils.mapper.JsonMapperUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import static org.jobrunr.utils.mapper.JsonMapperUtils.Json.FIELD_ACTUAL_CLASS_NAME;
import static org.jobrunr.utils.mapper.JsonMapperUtils.Json.FIELD_CLASS_NAME;
import static org.jobrunr.utils.reflection.ReflectionUtils.toClass;

public class JobParameterDeserializer extends StdDeserializer<JobParameter> {

    private final ObjectMapper objectMapper;

    protected JobParameterDeserializer() {
        super(JobParameter.class);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public JobParameter deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        final String className = node.get(FIELD_CLASS_NAME).asText();
        final String actualClassName = node.has(FIELD_ACTUAL_CLASS_NAME) ? node.get(FIELD_ACTUAL_CLASS_NAME).asText() : null;
        final JsonNode objectJsonNode = node.get("object");
        if (Path.class.getName().equals(className)) { // see https://github.com/FasterXML/jackson-databind/issues/2013
            return new JobParameter(className, Paths.get(objectJsonNode.asText().replace("file:", "")));
        } else {
            return getJobParameter(jsonParser, className, actualClassName, objectJsonNode);
        }
    }

    private JobParameter getJobParameter(JsonParser jsonParser, String className, String actualClassName, JsonNode objectJsonNode) throws JsonProcessingException {
        try {
            Class<Object> valueType = toClass(getActualClassName(className, actualClassName));
            if (objectJsonNode.isArray() && !Collection.class.isAssignableFrom(valueType) && !valueType.isArray()) { // why: regression form 4.0.1: See https://github.com/jobrunr/jobrunr/issues/254
                final JsonNode jsonNodeInArray = objectJsonNode.get(1);
                final Object object = jsonParser.getCodec().treeToValue(jsonNodeInArray, valueType);
                return new JobParameter(className, object);
            } else {
                try {
                    final Object object = jsonParser.getCodec().treeToValue(objectJsonNode, valueType);
                    return new JobParameter(className, object);
                } catch (MismatchedInputException e) { // last attempts
                    // is it an Enum?
                    if (valueType.isEnum()) {
                        ArrayNode arrayNode = (ArrayNode) jsonParser.getCodec().createArrayNode();
                        arrayNode.add(valueType.getName());
                        arrayNode.add(objectJsonNode);
                        final Object object = jsonParser.getCodec().treeToValue(arrayNode, valueType);
                        return new JobParameter(className, object);
                    } else {
                        // another try for Kotlin
                        final Object object = objectMapper.treeToValue(objectJsonNode, valueType);
                        return new JobParameter(className, object);
                    }
                }
            }
        } catch (Exception e) {
            return new JobParameter(new JobParameterNotDeserializableException(getActualClassName(className, actualClassName), e.getMessage()));
        }
    }

    public static String getActualClassName(String methodClassName, String actualClassName) {
        return JsonMapperUtils.getActualClassName(methodClassName, actualClassName, "sun.", "com.sun.");
    }
}
