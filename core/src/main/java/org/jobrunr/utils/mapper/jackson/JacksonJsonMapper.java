package org.jobrunr.utils.mapper.jackson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.jobrunr.JobRunrException;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.modules.JobRunrTimeModule;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.text.SimpleDateFormat;

import static org.jobrunr.utils.reflection.ReflectionUtils.newInstanceOrElse;

public class JacksonJsonMapper implements JsonMapper {

    private final ObjectMapper objectMapper;

    public JacksonJsonMapper() {
        objectMapper = new ObjectMapper()
                .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .registerModule(getModule())
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
            throw JobRunrException.shouldNotHappenException(e);
        }
    }

    @Override
    public void serialize(Writer writer, Object object) {
        try {
            objectMapper.writeValue(writer, object);
        } catch (IOException e) {
            throw JobRunrException.shouldNotHappenException(e);
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

    protected com.fasterxml.jackson.databind.Module getModule() {
        return newInstanceOrElse("com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", new JobRunrTimeModule());
    }
}
