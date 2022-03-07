package org.jobrunr.utils.mapper.jsonb;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.utils.mapper.jsonb.adapters.JobDetailsAdapter;
import org.jobrunr.utils.mapper.jsonb.adapters.JobHistoryAdapter;
import org.jobrunr.utils.mapper.jsonb.adapters.JobMetadataAdapter;
import org.jobrunr.utils.mapper.jsonb.serializer.*;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.adapter.JsonbAdapter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.jobrunr.utils.mapper.jsonb.NullSafeJsonBuilder.nullSafeJsonObjectBuilder;

public class JobAdapter implements JsonbAdapter<Job, JsonObject> {

    private final JobRunrJsonb jsonb;
    private final JobDetailsAdapter jobDetailsAdapter;
    private final JobHistoryAdapter jobHistoryAdapter;
    private final JobMetadataAdapter jobMetadataAdapter;

    public JobAdapter() {
        Jsonb jsonb = JsonbBuilder.create(new JsonbConfig()
                .withNullValues(true)
                .withPropertyVisibilityStrategy(new FieldAccessStrategy())
                .withSerializers(new PathTypeSerializer(), new FileTypeSerializer(), new DurationTypeSerializer())
                .withDeserializers(new PathTypeDeserializer(), new FileTypeDeserializer(), new DurationTypeDeserializer())
        );
        this.jsonb = new JobRunrJsonb(jsonb);
        this.jobDetailsAdapter = new JobDetailsAdapter(this.jsonb);
        this.jobHistoryAdapter = new JobHistoryAdapter(this.jsonb);
        this.jobMetadataAdapter = new JobMetadataAdapter(this.jsonb);
    }

    @Override
    public JsonObject adaptToJson(Job job) throws Exception {
        final JsonObjectBuilder builder = nullSafeJsonObjectBuilder()
                .add("id", job.getId())
                .add("jobName", job.getJobName())
                .add("jobSignature", job.getJobSignature())
                .add("version", job.getVersion())
                .add("metadata", jobMetadataAdapter.adaptToJson(job.getMetadata()))
                .add("jobDetails", jobDetailsAdapter.adaptToJson(job.getJobDetails()))
                .add("jobHistory", jobHistoryAdapter.adaptToJson(job.getJobStates()))
                .add("recurringJobId", job.getRecurringJobId().orElse(null));

        if (job.getId() != null) {
            builder.add("id", job.getId().toString());
        } else {
            builder.addNull("id");
        }
        return builder.build();
    }

    @Override
    public Job adaptFromJson(JsonObject jsonObject) throws Exception {
        final UUID id = jsonObject.isNull("id") ? null : UUID.fromString(jsonObject.getString("id"));
        final int version = jsonObject.getInt("version", 0);
        final JobDetails jobDetails = jobDetailsAdapter.adaptFromJson(jsonObject.getJsonObject("jobDetails"));
        final List<JobState> jobHistory = jobHistoryAdapter.adaptFromJson(jsonObject.getJsonArray("jobHistory"));
        final ConcurrentHashMap<String, Object> jobMetadata = jobMetadataAdapter.adaptFromJson(jsonObject.getJsonObject("metadata"));

        final Job job = new Job(id, version, jobDetails, jobHistory, jobMetadata);
        job.setJobName(jsonObject.getString("jobName"));
        job.setRecurringJobId(jsonObject.containsKey("recurringJobId") && !jsonObject.isNull("recurringJobId") ? jsonObject.getString("recurringJobId") : null);
        return job;
    }
}
