package org.jobrunr.utils.mapper.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.jobrunr.JobRunrException;
import org.jobrunr.utils.mapper.JobParameterJsonMapperException;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.modules.JobRunrModule;
import org.jobrunr.utils.mapper.jackson.modules.JobRunrTimeModule;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class JacksonJsonMapper implements JsonMapper {

    private final ObjectMapper objectMapper;

    public JacksonJsonMapper() {
        this(true);
    }

    public JacksonJsonMapper(boolean moduleAutoDiscover) {
        this(new ObjectMapper(), moduleAutoDiscover);
    }

    public JacksonJsonMapper(ObjectMapper objectMapper) {
        this(objectMapper, true);
    }

    public JacksonJsonMapper(ObjectMapper objectMapper, boolean moduleAutoDiscover) {
        this.objectMapper = initObjectMapper(objectMapper, moduleAutoDiscover);
    }

    protected ObjectMapper initObjectMapper(ObjectMapper objectMapper, boolean moduleAutoDiscover) {
        return objectMapper
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
                .configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
                .registerModules(findModules(moduleAutoDiscover))
                .setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"))
                .activateDefaultTypingAsProperty(LaissezFaireSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.NON_CONCRETE_AND_ARRAYS,
                        "@class");
    }

    @Override
    public String serialize(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JobParameterJsonMapperException("The job parameters are not serializable.", e);
        }
    }

    @Override
    public void serialize(OutputStream outputStream, Object object) {
        try {
            objectMapper.writeValue(outputStream, object);
        } catch (IOException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    @Override
    public <T> T deserialize(String serializedObjectAsString, Class<T> clazz) {
        try {
            return objectMapper.readValue(serializedObjectAsString, clazz);
        } catch (InvalidDefinitionException e) {
            throw JobRunrException.configurationException("Did you register all necessary Jackson Modules?", e);
        } catch (IOException e) {
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    private static List<Module> findModules(boolean moduleAutoDiscover) {
        List<Module> modules = moduleAutoDiscover ? ObjectMapper.findModules() : new ArrayList<>();
        if (modules.stream().noneMatch(isJSR310JavaTimeModule)) {
            modules.add(new JobRunrTimeModule());
        }
        if (modules.stream().noneMatch(isJobRunrModule)) {
            modules.add(new JobRunrModule());
        }
        return modules;
    }

    private static final Predicate<Module> isJSR310JavaTimeModule = m -> "jackson-datatype-jsr310".equals(m.getTypeId()) || "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule".equals(m.getTypeId());
    private static final Predicate<Module> isJobRunrModule = m -> "org.jobrunr.utils.mapper.jackson.modules.JobRunrModule".equals(m.getTypeId());
}
