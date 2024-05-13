package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.BitSet;

public class BitSetDeserializer extends JsonDeserializer<BitSet> {
    @Override
    public BitSet deserialize(com.fasterxml.jackson.core.JsonParser p, com.fasterxml.jackson.databind.DeserializationContext ctxt) throws IOException {
        BitSet bitSet = new BitSet();
        int index = 0;
        while (p.nextToken() != com.fasterxml.jackson.core.JsonToken.END_ARRAY) {
            if (p.getBooleanValue()) {
                bitSet.set(index);
            }
            index++;
        }
        return bitSet;
    }
}
