package org.jobrunr.utils.mapper.jsonb;

import org.jobrunr.dashboard.ui.model.RecurringJobUIModel;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.utils.mapper.jsonb.adapters.JobDetailsAdapter;
import org.jobrunr.utils.mapper.jsonb.serializer.DurationTypeDeserializer;
import org.jobrunr.utils.mapper.jsonb.serializer.DurationTypeSerializer;
import org.jobrunr.utils.mapper.jsonb.serializer.PathTypeDeserializer;
import org.jobrunr.utils.mapper.jsonb.serializer.PathTypeSerializer;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.adapter.JsonbAdapter;

import static org.jobrunr.utils.mapper.jsonb.NullSafeJsonBuilder.nullSafeJsonObjectBuilder;

public class RecurringJobAdapter implements JsonbAdapter<RecurringJob, JsonObject> {

    private final JobRunrJsonb jsonb;
    private final JobDetailsAdapter jobDetailsAdapter;

    public RecurringJobAdapter() {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .withNullValues(true)
                .withPropertyVisibilityStrategy(new FieldAccessStrategy())
                .withSerializers(new PathTypeSerializer(), new DurationTypeSerializer())
                .withDeserializers(new PathTypeDeserializer(), new DurationTypeDeserializer())
        );
        this.jsonb = new JobRunrJsonb(jsonb);
        jobDetailsAdapter = new JobDetailsAdapter(this.jsonb);
    }

    @Override
    public JsonObject adaptToJson(RecurringJob recurringJob) throws Exception {
        final JsonObjectBuilder builder = nullSafeJsonObjectBuilder()
                .add("id", recurringJob.getId())
                .add("jobName", recurringJob.getJobName())
                .add("jobSignature", recurringJob.getJobSignature())
                .add("version", recurringJob.getVersion())
                .add("cronExpression", recurringJob.getCronExpression())
                .add("zoneId", recurringJob.getZoneId())
                .add("jobDetails", jobDetailsAdapter.adaptToJson(recurringJob.getJobDetails()));
        if (recurringJob instanceof RecurringJobUIModel) {
            builder.add("nextRun", recurringJob.getNextRun().toString());
        }
        return builder.build();
    }

    @Override
    public RecurringJob adaptFromJson(JsonObject jsonObject) throws Exception {
        final RecurringJob recurringJob = new RecurringJob(
                jsonObject.getString("id"),
                jobDetailsAdapter.adaptFromJson(jsonObject.getJsonObject("jobDetails")),
                jsonObject.getString("cronExpression"),
                jsonObject.getString("zoneId")
        );
        recurringJob.setJobName(jsonObject.getString("jobName"));
        return recurringJob;
    }

}
