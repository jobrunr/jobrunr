package org.jobrunr.utils.mapper.jsonb.adapters;

import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.utils.mapper.jsonb.JobRunrJsonb;

import javax.json.*;
import javax.json.bind.adapter.JsonbAdapter;
import java.util.ArrayList;
import java.util.List;

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
        for (JobParameter jobState : jobDetails.getJobParameters()) {
            final JsonObject object = nullSafeJsonObjectBuilder(jsonb, jobState).build();
            parametersJsonArray.add(object);
        }

        return nullSafeJsonObjectBuilder()
                .add("className", jobDetails.getClassName())
                .add("staticFieldName", jobDetails.getStaticFieldName().orElse(null))
                .add("methodName", jobDetails.getMethodName())
                .add("jobParameters", parametersJsonArray.build())
                .build();
    }

    @Override
    public JobDetails adaptFromJson(JsonObject jsonObject) throws Exception {
        return new JobDetails(
                jsonObject.getString("className"),
                jsonObject.isNull("staticFieldName") ? null : jsonObject.getString("staticFieldName"),
                jsonObject.getString("methodName"),
                getJobDetailsParameters(jsonObject.getJsonArray("jobParameters"))
        );
    }

    private List<JobParameter> getJobDetailsParameters(JsonArray jobParameters) {
        List<JobParameter> result = new ArrayList<>();
        for (JsonValue jsonValue : jobParameters) {
            final JsonObject jsonObject = jsonValue.asJsonObject();
            String methodClassName = jsonObject.getString("className");
            String actualClassName = jsonObject.getString("actualClassName", null);
            Object object = jsonb.fromJsonValue(jsonObject.get("object"), toClass(getActualClassName(methodClassName, actualClassName)));
            result.add(new JobParameter(methodClassName, object));
        }
        return result;
    }
}
