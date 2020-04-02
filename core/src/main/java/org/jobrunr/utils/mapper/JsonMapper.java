package org.jobrunr.utils.mapper;

import java.io.OutputStream;
import java.io.Writer;

public interface JsonMapper {

    String serialize(Object object);

    void serialize(Writer writer, Object object);

    void serialize(OutputStream outputStream, Object object);

    <T> T deserialize(String serializedObjectAsString, Class<T> clazz);

}
