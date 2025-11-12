package org.jobrunr.utils.mapper.jackson.modules;

import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.exceptions.JobParameterNotDeserializableException;
import org.jobrunr.utils.mapper.JsonMapperUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.node.ArrayNode;

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
    public JobParameter deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws JacksonException {
        JsonNode node = objectMapper.readTree(jsonParser);
        final String className = node.get(FIELD_CLASS_NAME).asText();
        final String actualClassName = node.has(FIELD_ACTUAL_CLASS_NAME) ? node.get(FIELD_ACTUAL_CLASS_NAME).asText() : null;
        final JsonNode objectJsonNode = node.get("object");
        if (Path.class.getName().equals(className)) { // see https://github.com/FasterXML/jackson-databind/issues/2013
            return new JobParameter(className, Paths.get(objectJsonNode.asText().replace("file:", "")));
        } else {
            return getJobParameter(jsonParser, className, actualClassName, objectJsonNode);
        }
    }

    private JobParameter getJobParameter(JsonParser jsonParser, String className, String actualClassName, JsonNode objectJsonNode) {
        try {
            Class<Object> valueType = toClass(getActualClassName(className, actualClassName));
            if (objectJsonNode.isArray() && !Collection.class.isAssignableFrom(valueType) && !valueType.isArray()) { // why: regression form 4.0.1: See https://github.com/jobrunr/jobrunr/issues/254
                final JsonNode jsonNodeInArray = objectJsonNode.get(1);
                final Object object = objectMapper.treeToValue(jsonNodeInArray, valueType);
                return new JobParameter(className, object);
            } else {
                try {
                    final Object object = objectMapper.treeToValue(objectJsonNode, valueType);
                    return new JobParameter(className, object);
                } catch (MismatchedInputException e) { // last attempts
                    // is it an Enum?
                    if (valueType.isEnum()) {
                        ArrayNode arrayNode = (ArrayNode) objectMapper.createArrayNode();
                        arrayNode.add(valueType.getName());
                        arrayNode.add(objectJsonNode);
                        final Object object = objectMapper.treeToValue(arrayNode, valueType);
                        return new JobParameter(className, object);
                    } else {
                        // another try for Kotlin
                        final Object object = objectMapper.treeToValue(objectJsonNode, valueType);
                        return new JobParameter(className, object);
                    }
                }
            }
        } catch (Exception e) {
            return new JobParameter(className, actualClassName, objectJsonNode, new JobParameterNotDeserializableException(className, e));
        }
    }

    public static String getActualClassName(String methodClassName, String actualClassName) {
        return JsonMapperUtils.getActualClassName(methodClassName, actualClassName, "sun.", "com.sun.");
    }
}
