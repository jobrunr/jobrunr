package org.jobrunr.kotlin.utils.mapper;

import org.jobrunr.utils.mapper.JsonMapper;

import java.io.OutputStream;

// To test class loading of KotlinxSerializationJsonMapper that's part of another package we do not want to depend on directly.
public class KotlinxSerializationJsonMapper implements JsonMapper {

    @Override
    public String serialize(Object object) {
        return "";
    }

    @Override
    public void serialize(OutputStream outputStream, Object object) {

    }

    @Override
    public <T> T deserialize(String serializedObjectAsString, Class<T> clazz) {
        return null;
    }
}
