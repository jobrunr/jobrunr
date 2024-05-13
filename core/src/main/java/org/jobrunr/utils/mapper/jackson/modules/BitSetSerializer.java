package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.BitSet;

public class BitSetSerializer extends JsonSerializer<BitSet> {
    @Override
    public void serialize(BitSet bitSet, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartArray();
        for (int i = 0; i < bitSet.length(); i++) {
            gen.writeBoolean(bitSet.get(i));
        }
        gen.writeEndArray();
    }
}

