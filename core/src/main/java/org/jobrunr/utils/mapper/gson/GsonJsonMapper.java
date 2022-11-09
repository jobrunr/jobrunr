package org.jobrunr.utils.mapper.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.JobParameter;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.utils.mapper.JobParameterJsonMapperException;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.metadata.VersionRetriever;
import org.jobrunr.utils.reflection.ReflectionUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;

public class GsonJsonMapper implements JsonMapper {

    private final Gson gson;

    public GsonJsonMapper() {
        this(new GsonBuilder());
    }

    public GsonJsonMapper(GsonBuilder gsonBuilder) {
        this.gson = initGson(gsonBuilder);
        fixGsonNotBeingExtensible(gson);
    }

    public GsonJsonMapper(Gson gson) {
        this.gson = gson;
        fixGsonNotBeingExtensible(gson);
    }

    protected Gson initGson(GsonBuilder gsonBuilder) {
        return gsonBuilder
                .serializeNulls()
                .registerTypeAdapterFactory(RuntimeClassNameTypeAdapterFactory.of(JobState.class))
                .registerTypeAdapterFactory(RuntimeClassNameTypeAdapterFactory.of(Map.class))
                .registerTypeAdapterFactory(RuntimeClassNameTypeAdapterFactory.of(JobContext.Metadata.class))
                .registerTypeHierarchyAdapter(Path.class, new PathAdapter().nullSafe())
                .registerTypeAdapter(File.class, new FileAdapter().nullSafe())
                .registerTypeAdapter(Instant.class, new InstantAdapter().nullSafe())
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter().nullSafe())
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter().nullSafe())
                .registerTypeAdapter(Duration.class, new DurationAdapter())
                .registerTypeAdapter(JobParameter.class, new JobParameterDeserializer())
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

    // I'm really sorry for this
    // see https://github.com/google/gson/issues/1177
    // when deserializing, GSON does not create correct types for items in collections
    private void fixGsonNotBeingExtensible(Gson gson) {
        try {
            final Field factories = ReflectionUtils.getField(Gson.class, "factories");
            ReflectionUtils.makeAccessible(factories);
            final List o = new ArrayList<TypeAdapterFactory>((Collection<? extends TypeAdapterFactory>) factories.get(gson));
            if (!o.get(1).getClass().getName().contains("ObjectTypeAdapter"))
                throw JobRunrException.shouldNotHappenException(String.format("It looks like you are running a Gson version (%s) which is not compatible with JobRunr", VersionRetriever.getVersion(Gson.class)));
            o.set(1, ClassNameObjectTypeAdapter.FACTORY);
            factories.set(gson, unmodifiableList(o));
        } catch (ReflectiveOperationException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }
}
