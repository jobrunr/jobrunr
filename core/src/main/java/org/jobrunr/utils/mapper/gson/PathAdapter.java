package org.jobrunr.utils.mapper.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class PathAdapter extends TypeAdapter<Path> {
    @Override
    public void write(final JsonWriter jsonWriter, final Path path) throws IOException {
        jsonWriter.value(path.toString());
    }

    @Override
    public Path read(final JsonReader jsonReader) throws IOException {
        return Paths.get(jsonReader.nextString());
    }
}