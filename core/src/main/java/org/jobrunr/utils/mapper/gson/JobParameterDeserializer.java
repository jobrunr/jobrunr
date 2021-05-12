package org.jobrunr.utils.mapper.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.utils.reflection.ReflectionUtils;

import java.lang.reflect.Type;

import static org.jobrunr.utils.mapper.JsonMapperUtils.Json.FIELD_ACTUAL_CLASS_NAME;
import static org.jobrunr.utils.mapper.JsonMapperUtils.Json.FIELD_CLASS_NAME;
import static org.jobrunr.utils.mapper.JsonMapperUtils.getActualClassName;

public class JobParameterDeserializer implements JsonDeserializer<JobParameter> {

    @Override
    public JobParameter deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String jobParameterMethodType = jsonObject.get(FIELD_CLASS_NAME).getAsString();
        String jobParameterActualType = jsonObject.has(FIELD_ACTUAL_CLASS_NAME) ? jsonObject.get(FIELD_ACTUAL_CLASS_NAME).getAsString() : null;
        return new JobParameter(jobParameterMethodType, deserializeToObject(context, getActualClassName(jobParameterMethodType, jobParameterActualType), jsonObject.get("object")));
    }

    private Object deserializeToObject(JsonDeserializationContext context, String type, JsonElement jsonElement) {
        return context.deserialize(jsonElement, ReflectionUtils.toClass(type));
    }
}
