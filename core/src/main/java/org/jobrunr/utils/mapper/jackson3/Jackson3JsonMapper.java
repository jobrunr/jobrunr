package org.jobrunr.utils.mapper.jackson3;

import org.jobrunr.utils.mapper.JsonMapper;

import java.io.OutputStream;

public class Jackson3JsonMapper implements JsonMapper {

    public Jackson3JsonMapper() {
        throw new UnsupportedOperationException("You need Java 17 or higher to use Jackson3JsonMapper.");
    }

    public Jackson3JsonMapper(tools.jackson.databind.json.JsonMapper.Builder builder) {
        throw new UnsupportedOperationException("You need Java 17 or higher to use Jackson3JsonMapper.");
    }

    @Override
    public String serialize(Object object) {
        throw new UnsupportedOperationException("You need Java 17 or higher to use Jackson3JsonMapper.");
    }

    @Override
    public void serialize(OutputStream outputStream, Object object) {
        throw new UnsupportedOperationException("You need Java 17 or higher to use Jackson3JsonMapper.");
    }

    @Override
    public <T> T deserialize(String serializedObjectAsString, Class<T> clazz) {
        throw new UnsupportedOperationException("You need Java 17 or higher to use Jackson3JsonMapper.");
    }
}
