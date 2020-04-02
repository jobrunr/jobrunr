package org.jobrunr.utils.mapper.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;

public final class InstantAdapter extends TypeAdapter<Instant> {
    @Override
    public void write(final JsonWriter jsonWriter, final Instant instant) throws IOException {
        jsonWriter.value(instant.toString());
    }

    @Override
    public Instant read(final JsonReader jsonReader) throws IOException {
        return Instant.parse(jsonReader.nextString());
    }
}