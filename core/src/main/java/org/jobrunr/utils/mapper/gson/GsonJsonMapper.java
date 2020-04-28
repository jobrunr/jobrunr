package org.jobrunr.utils.mapper.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.JobContext;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.utils.mapper.JsonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public class GsonJsonMapper implements JsonMapper {

    private final Gson gson;

    public GsonJsonMapper() {
        gson = new GsonBuilder()
                .serializeNulls()
                .registerTypeAdapterFactory(RuntimeClassNameTypeAdapterFactory.of(JobState.class))
                .registerTypeAdapterFactory(RuntimeClassNameTypeAdapterFactory.of(Map.class))
                .registerTypeAdapterFactory(RuntimeClassNameTypeAdapterFactory.of(JobContext.Metadata.class))
                .registerTypeHierarchyAdapter(Path.class, new PathAdapter().nullSafe())
                .registerTypeAdapter(Instant.class, new InstantAdapter().nullSafe())
                .registerTypeAdapter(Duration.class, new DurationAdapter())
                .registerTypeAdapter(JobParameter.class, new JobParameterDeserializer())
                .create();
    }

    @Override
    public String serialize(Object object) {
        return gson.toJson(object);
    }

    @Override
    public void serialize(Writer writer, Object object) {
        gson.toJson(object, writer);
    }

    @Override
    public void serialize(OutputStream outputStream, Object object) {
        try (final OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {
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
