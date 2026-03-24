package org.jobrunr.utils.mapper.gson;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.bind.ObjectTypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class ClassNameObjectTypeAdapter extends TypeAdapter<Object> {

    public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            return type.getRawType() == Object.class ? (TypeAdapter<T>) new ClassNameObjectTypeAdapter(gson) : null;
        }
    };

    private final Gson gson;

    ClassNameObjectTypeAdapter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void write(JsonWriter out, Object value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            TypeAdapter<Object> typeAdapter = (TypeAdapter<Object>) gson.getAdapter(value.getClass());
            if (typeAdapter instanceof ObjectTypeAdapter) {
                out.beginObject();
                out.endObject();
                return;
            }

            typeAdapter.write(out, value);
        }
    }

    @Override
    public Object read(JsonReader in) throws IOException {
        return GsonJsonElementUtils.deserializeElement(
                JsonParser.parseReader(in),
                (clazz, json) -> gson.getAdapter(TypeToken.get(clazz)).fromJsonTree(json)
        );
    }
}
