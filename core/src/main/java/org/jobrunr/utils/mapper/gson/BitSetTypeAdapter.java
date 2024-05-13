package org.jobrunr.utils.mapper.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.BitSet;

public class BitSetTypeAdapter extends TypeAdapter<BitSet> {

    @Override
    public void write(JsonWriter out, BitSet bitSet) throws IOException {
        out.beginArray();
        for (int i = 0; i < bitSet.length(); i++) {
            out.value(bitSet.get(i));
        }
        out.endArray();
    }

    @Override
    public BitSet read(JsonReader in) throws IOException {
        BitSet bitSet = new BitSet();
        in.beginArray();
        int index = 0;
        while (in.hasNext()) {
            if (in.nextBoolean()) {
                bitSet.set(index);
            }
            index++;
        }
        in.endArray();
        return bitSet;
    }
}
