package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import java.io.IOException;
import java.time.Year;

public class YearKeyDeserializer extends KeyDeserializer {
    @Override
    public Object deserializeKey(String key, DeserializationContext deserializationContext) throws IOException {
        return Year.parse(key);
    }
}
