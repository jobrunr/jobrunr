package org.jobrunr.utils.mapper.gson;


import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.states.JobState;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class JobTypeAdapter extends TypeAdapter<Job> {
    private static final TypeToken<List<JobState>> JOB_HISTORY_TYPE = new TypeToken<List<JobState>>() {};
    private static final TypeToken<List<String>> LABELS_TYPE = new TypeToken<List<String>>() {};

    private final Gson gson;
    private final ClassNameObjectTypeAdapter objectAdapter;
    private final TypeAdapter<JobDetails> jobDetailsAdapter;
    private final TypeAdapter<List<JobState>> jobHistoryAdapter;
    private final TypeAdapter<List<String>> labelsAdapter;

    public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            return Job.class.isAssignableFrom(type.getRawType()) ? (TypeAdapter<T>) new JobTypeAdapter(gson).nullSafe() : null;
        }
    };

    JobTypeAdapter(Gson gson) {
        this.gson = gson;
        this.objectAdapter = new ClassNameObjectTypeAdapter(gson);
        this.jobDetailsAdapter = gson.getAdapter(JobDetails.class);
        this.jobHistoryAdapter = gson.getAdapter(JOB_HISTORY_TYPE);
        this.labelsAdapter = gson.getAdapter(LABELS_TYPE);
    }

    @Override
    public void write(JsonWriter out, Job value) throws IOException {
        gson.getDelegateAdapter(FACTORY, TypeToken.get(Job.class)).write(out, value);
    }

    @Override
    public Job read(JsonReader in) throws IOException {
        JsonObject jsonObject = JsonParser.parseReader(in).getAsJsonObject();

        UUID id = UUID.fromString(jsonObject.get("id").getAsString());
        int version = jsonObject.get("version").getAsInt();
        JobDetails jobDetails = jobDetailsAdapter.fromJsonTree(jsonObject.get("jobDetails"));
        List<JobState> jobHistory = jobHistoryAdapter.fromJsonTree(jsonObject.get("jobHistory"));
        ConcurrentHashMap<String, Object> metadata = cast(objectAdapter.fromJsonTree(jsonObject.get("metadata")));

        Job job = new Job(id, version, jobDetails, jobHistory, metadata);
        job.setJobName(jsonObject.get("jobName").getAsString());
        if (GsonJsonElementUtils.isJsonNonNullElement(jsonObject.get("labels"))) job.setLabels(labelsAdapter.fromJsonTree(jsonObject.get("labels")));
        job.setAmountOfRetries(GsonJsonElementUtils.getAsIntegerOrNull(jsonObject.get("amountOfRetries")));
        job.setRecurringJobId(GsonJsonElementUtils.getAsStringOrNull(jsonObject.get("recurringJobId")));
        return job;
    }
}