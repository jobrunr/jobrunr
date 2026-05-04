package org.jobrunr.utils.mapper.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;

import java.lang.reflect.Type;
import java.util.List;

import static org.jobrunr.utils.mapper.JsonMapperUtils.Json.FIELD_CACHEABLE;
import static org.jobrunr.utils.mapper.JsonMapperUtils.Json.FIELD_CLASS_NAME;
import static org.jobrunr.utils.mapper.JsonMapperUtils.Json.FIELD_JOB_PARAMETERS;
import static org.jobrunr.utils.mapper.JsonMapperUtils.Json.FIELD_METHOD_NAME;
import static org.jobrunr.utils.mapper.JsonMapperUtils.Json.FIELD_STATIC_FIELD_NAME;

public class JobDetailsDeserializer implements JsonDeserializer<JobDetails> {
    private static final Type JOB_PARAMETERS_TYPE = new TypeToken<List<JobParameter>>() {}.getType();

    @Override
    public JobDetails deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        final JobDetails jobDetails = new JobDetails(
                jsonObject.get(FIELD_CLASS_NAME).getAsString(),
                GsonJsonElementUtils.getAsStringOrNull(jsonObject.get(FIELD_STATIC_FIELD_NAME)),
                jsonObject.get(FIELD_METHOD_NAME).getAsString(),
                jsonObject.has(FIELD_JOB_PARAMETERS) ? context.deserialize(jsonObject.get(FIELD_JOB_PARAMETERS), JOB_PARAMETERS_TYPE) : null
        );
        jobDetails.setCacheable(jsonObject.get(FIELD_CACHEABLE).getAsBoolean());
        return jobDetails;
    }
}
