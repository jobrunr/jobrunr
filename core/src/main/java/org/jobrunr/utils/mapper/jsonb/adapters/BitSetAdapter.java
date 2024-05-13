package org.jobrunr.utils.mapper.jsonb.adapters;

import jakarta.json.bind.adapter.JsonbAdapter;

import java.util.BitSet;

public class BitSetAdapter implements JsonbAdapter<BitSet, String> {

    @Override
    public String adaptToJson(BitSet bitSet) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bitSet.length(); i++) {
            sb.append(bitSet.get(i) ? '1' : '0');
        }
        return sb.toString();
    }

    @Override
    public BitSet adaptFromJson(String str) {
        BitSet bitSet = new BitSet(str.length());
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '1') {
                bitSet.set(i);
            }
        }
        return bitSet;
    }
}