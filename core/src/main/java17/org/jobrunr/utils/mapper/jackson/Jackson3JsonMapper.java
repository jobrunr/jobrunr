package org.jobrunr.utils.mapper.jackson;


import org.jobrunr.utils.mapper.JsonMapper;

import java.io.OutputStream;

public class Jackson3JsonMapper implements JsonMapper {

    private final tools.jackson.databind.json.JsonMapper jsonMapper;

    public Jackson3JsonMapper() {
        jsonMapper = new tools.jackson.databind.json.JsonMapper();
    }

    @Override
    public String serialize(Object object) {
        return jsonMapper.writeValueAsString(object);
    }

    @Override
    public void serialize(OutputStream outputStream, Object object) {
        jsonMapper.writeValue(outputStream, object);
    }

    @Override
    public <T> T deserialize(String serializedObjectAsString, Class<T> clazz) {
        return jsonMapper.readValue(serializedObjectAsString, clazz);
    }
}
