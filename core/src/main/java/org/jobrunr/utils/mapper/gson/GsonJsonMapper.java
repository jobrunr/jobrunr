package org.jobrunr.utils.mapper.gson;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.JobDetails;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.utils.mapper.JobParameterJsonMapperException;
import org.jobrunr.utils.mapper.JsonMapper;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GsonJsonMapper implements JsonMapper {

    private final Gson gson;

    public GsonJsonMapper() {
        this(new GsonBuilder());
    }

    public GsonJsonMapper(GsonBuilder gsonBuilder) {
        this.gson = initGson(gsonBuilder);
    }

    public GsonJsonMapper(Gson gson) {
        this.gson = gson;
    }

    protected Gson initGson(GsonBuilder gsonBuilder) {
        return gsonBuilder
                .registerTypeAdapterFactory(RuntimeClassNameTypeAdapterFactory.of(JobState.class))
                .registerTypeAdapterFactory(RuntimeClassNameTypeAdapterFactory.of(Map.class))
                .registerTypeAdapterFactory(RuntimeClassNameTypeAdapterFactory.of(JobContext.Metadata.class))
                .registerTypeAdapterFactory(JobTypeAdapter.FACTORY)
                .registerTypeHierarchyAdapter(Path.class, new PathAdapter().nullSafe())
                .registerTypeAdapter(File.class, new FileAdapter().nullSafe())
                .registerTypeAdapter(Instant.class, new InstantAdapter().nullSafe())
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter().nullSafe())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter().nullSafe())
                .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter().nullSafe())
                .registerTypeAdapter(Duration.class, new DurationAdapter().nullSafe())
                .registerTypeAdapter(JobDetails.class, new JobDetailsDeserializer())
                .registerTypeAdapter(JobParameter.class, new JobParameterDeserializer())
                .addDeserializationExclusionStrategy(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return JobState.class.isAssignableFrom(f.getDeclaringClass()) && f.getName().equals("state");
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .create();
    }

    @Override
    public String serialize(Object object) {
        try {
            return gson.toJson(object);
        } catch (Exception e) {
            throw new JobParameterJsonMapperException("The job parameters are not serializable.", e);
        }
    }

    @Override
    public void serialize(OutputStream outputStream, Object object) {
        try (final OutputStreamWriter writer = new OutputStreamWriter(outputStream, UTF_8)) {
            gson.toJson(object, writer);
        } catch (IOException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    @Override
    public <T> T deserialize(String serializedObjectAsString, Class<T> clazz) {
        return gson.fromJson(serializedObjectAsString, clazz);
    }
}
