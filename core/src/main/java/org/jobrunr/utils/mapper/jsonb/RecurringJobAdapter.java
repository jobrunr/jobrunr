package org.jobrunr.utils.mapper.jsonb;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.adapter.JsonbAdapter;
import org.jobrunr.dashboard.ui.model.RecurringJobUIModel;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.RecurringJob.CreatedBy;
import org.jobrunr.utils.mapper.jsonb.adapters.JobDetailsAdapter;
import org.jobrunr.utils.mapper.jsonb.adapters.JobLabelsAdapter;
import org.jobrunr.utils.mapper.jsonb.serializer.DurationTypeDeserializer;
import org.jobrunr.utils.mapper.jsonb.serializer.DurationTypeSerializer;
import org.jobrunr.utils.mapper.jsonb.serializer.FileTypeDeserializer;
import org.jobrunr.utils.mapper.jsonb.serializer.FileTypeSerializer;
import org.jobrunr.utils.mapper.jsonb.serializer.PathTypeDeserializer;
import org.jobrunr.utils.mapper.jsonb.serializer.PathTypeSerializer;

import static org.jobrunr.utils.mapper.jsonb.NullSafeJsonBuilder.nullSafeJsonObjectBuilder;

public class RecurringJobAdapter implements JsonbAdapter<RecurringJob, JsonObject> {

    private final JobRunrJsonb jsonb;
    private final JobLabelsAdapter jobLabelsAdapter;
    private final JobDetailsAdapter jobDetailsAdapter;

    public RecurringJobAdapter() {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .withPropertyVisibilityStrategy(new FieldAccessStrategy())
                .withSerializers(new PathTypeSerializer(), new FileTypeSerializer(), new DurationTypeSerializer())
                .withDeserializers(new PathTypeDeserializer(), new FileTypeDeserializer(), new DurationTypeDeserializer())
        );
        this.jsonb = new JobRunrJsonb(jsonb);
        jobLabelsAdapter = new JobLabelsAdapter(this.jsonb);
        jobDetailsAdapter = new JobDetailsAdapter(this.jsonb);
    }

    @Override
    public JsonObject adaptToJson(RecurringJob recurringJob) throws Exception {
        final JsonObjectBuilder builder = nullSafeJsonObjectBuilder()
                .add("id", recurringJob.getId())
                .add("jobName", recurringJob.getJobName())
                .add("amountOfRetries", recurringJob.getAmountOfRetries())
                .add("labels", jobLabelsAdapter.adaptToJson(recurringJob.getLabels()))
                .add("jobSignature", recurringJob.getJobSignature())
                .add("version", recurringJob.getVersion())
                .add("scheduleExpression", recurringJob.getScheduleExpression())
                .add("zoneId", recurringJob.getZoneId())
                .add("jobDetails", jobDetailsAdapter.adaptToJson(recurringJob.getJobDetails()))
                .add("createdBy", recurringJob.getCreatedBy().toString())
                .add("createdAt", recurringJob.getCreatedAt().toString());

        if (recurringJob instanceof RecurringJobUIModel) {
            builder.add("nextRun", recurringJob.getNextRun().toString());
        }
        return builder.build();
    }

    @Override
    public RecurringJob adaptFromJson(JsonObject jsonObject) throws Exception {
        String createdBy = jsonObject.getString("createdBy", CreatedBy.API.name());
        final RecurringJob recurringJob = new RecurringJob(
                jsonObject.getString("id"),
                jobDetailsAdapter.adaptFromJson(jsonObject.getJsonObject("jobDetails")),
                jsonObject.getString("scheduleExpression"),
                jsonObject.getString("zoneId"),
                CreatedBy.valueOf(createdBy),
                jsonObject.getString("createdAt")
        );
        recurringJob.setJobName(jsonObject.getString("jobName"));
        recurringJob.setLabels(jobLabelsAdapter.adaptFromJson(jsonObject.getJsonArray("labels")));
        return recurringJob;
    }

}
