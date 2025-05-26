package org.jobrunr.utils.mapper.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.exceptions.JobParameterNotDeserializableException;
import org.jobrunr.utils.reflection.ReflectionUtils;

import java.lang.reflect.Type;

import static org.jobrunr.utils.mapper.JsonMapperUtils.Json.FIELD_ACTUAL_CLASS_NAME;
import static org.jobrunr.utils.mapper.JsonMapperUtils.Json.FIELD_CLASS_NAME;
import static org.jobrunr.utils.mapper.JsonMapperUtils.getActualClassName;

public class JobParameterDeserializer implements JsonDeserializer<JobParameter> {

    @Override
    public JobParameter deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String className = jsonObject.get(FIELD_CLASS_NAME).getAsString();
        String actualClassName = jsonObject.has(FIELD_ACTUAL_CLASS_NAME) ? jsonObject.get(FIELD_ACTUAL_CLASS_NAME).getAsString() : null;
        try {
            return new JobParameter(className, deserializeToObject(context, getActualClassName(className, actualClassName), jsonObject.get("object")));
        } catch (Exception e) {
            return new JobParameter(className, actualClassName, jsonObject.get("object"), new JobParameterNotDeserializableException(className, e));
        }
    }

    private Object deserializeToObject(JsonDeserializationContext context, String type, JsonElement jsonElement) {
        return context.deserialize(jsonElement, ReflectionUtils.toClass(type));
    }
}
