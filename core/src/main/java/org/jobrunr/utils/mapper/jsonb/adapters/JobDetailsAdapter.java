package org.jobrunr.utils.mapper.jsonb.adapters;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.utils.mapper.JobParameterJsonMapperException;
import org.jobrunr.utils.mapper.jsonb.JobRunrJsonb;

import javax.json.*;
import javax.json.bind.adapter.JsonbAdapter;
import java.util.ArrayList;
import java.util.List;

import static org.jobrunr.utils.mapper.JsonMapperUtils.Json.FIELD_ACTUAL_CLASS_NAME;
import static org.jobrunr.utils.mapper.JsonMapperUtils.Json.FIELD_CACHEABLE;
import static org.jobrunr.utils.mapper.JsonMapperUtils.Json.FIELD_CLASS_NAME;
import static org.jobrunr.utils.mapper.JsonMapperUtils.Json.FIELD_JOB_PARAMETERS;
import static org.jobrunr.utils.mapper.JsonMapperUtils.Json.FIELD_METHOD_NAME;
import static org.jobrunr.utils.mapper.JsonMapperUtils.Json.FIELD_STATIC_FIELD_NAME;
import static org.jobrunr.utils.mapper.JsonMapperUtils.getActualClassName;
import static org.jobrunr.utils.mapper.jsonb.NullSafeJsonBuilder.nullSafeJsonObjectBuilder;
import static org.jobrunr.utils.reflection.ReflectionUtils.toClass;

public class JobDetailsAdapter implements JsonbAdapter<JobDetails, JsonObject> {
    private final JobRunrJsonb jsonb;

    public JobDetailsAdapter(JobRunrJsonb jsonb) {
        this.jsonb = jsonb;
    }

    @Override
    public JsonObject adaptToJson(JobDetails jobDetails) throws Exception {
        final JsonArrayBuilder parametersJsonArray = Json.createArrayBuilder();
        try {
            for (JobParameter jobState : jobDetails.getJobParameters()) {
                final JsonObject object = nullSafeJsonObjectBuilder(jsonb, jobState).build();
                parametersJsonArray.add(object);
            }
        } catch (Exception e) {
            throw new JobParameterJsonMapperException("The job parameters are not serializable.", e);
        }

        return nullSafeJsonObjectBuilder()
                .add(FIELD_CACHEABLE, jobDetails.getCacheable())
                .add(FIELD_CLASS_NAME, jobDetails.getClassName())
                .add(FIELD_STATIC_FIELD_NAME, jobDetails.getStaticFieldName())
                .add(FIELD_METHOD_NAME, jobDetails.getMethodName())
                .add(FIELD_JOB_PARAMETERS, parametersJsonArray.build())
                .build();
    }

    @Override
    public JobDetails adaptFromJson(JsonObject jsonObject) throws Exception {
        final JobDetails jobDetails = new JobDetails(
                jsonObject.getString(FIELD_CLASS_NAME),
                jsonObject.isNull(FIELD_STATIC_FIELD_NAME) ? null : jsonObject.getString(FIELD_STATIC_FIELD_NAME),
                jsonObject.getString(FIELD_METHOD_NAME),
                getJobDetailsParameters(jsonObject.getJsonArray(FIELD_JOB_PARAMETERS))
        );
        jobDetails.setCacheable(jsonObject.getBoolean(FIELD_CACHEABLE));
        return jobDetails;
    }

    private List<JobParameter> getJobDetailsParameters(JsonArray jobParameters) {
        List<JobParameter> result = new ArrayList<>();
        for (JsonValue jsonValue : jobParameters) {
            final JsonObject jsonObject = jsonValue.asJsonObject();
            String methodClassName = jsonObject.getString(FIELD_CLASS_NAME);
            String actualClassName = jsonObject.getString(FIELD_ACTUAL_CLASS_NAME, null);
            Object object = jsonb.fromJsonValue(jsonObject.get("object"), toClass(getActualClassName(methodClassName, actualClassName)));
            result.add(new JobParameter(methodClassName, object));
        }
        return result;
    }
}
