package org.jobrunr.utils.mapper.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.IOException;

public class FileAdapter extends TypeAdapter<File> {
    @Override
    public void write(JsonWriter jsonWriter, File file) throws IOException {
        jsonWriter.value(file.toString());
    }

    @Override
    public File read(JsonReader jsonReader) throws IOException {
        return new File(jsonReader.nextString());
    }
}
