package org.jobrunr.utils.mapper.gson;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.internal.bind.ObjectTypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jobrunr.utils.mapper.gson.RuntimeClassNameTypeAdapterFactory.TYPE_FIELD_NAME;
import static org.jobrunr.utils.reflection.ReflectionUtils.toClass;

public class ClassNameObjectTypeAdapter extends TypeAdapter<Object> {

    public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            return type.getRawType() == Object.class ? (TypeAdapter<T>) new ClassNameObjectTypeAdapter(gson) : null;
        }
    };

    private final Gson gson;

    ClassNameObjectTypeAdapter(Gson gson) {
        this.gson = gson;
    }

    public Object read(JsonReader in) throws IOException {
        JsonToken token = in.peek();
        switch (token) {
            case BEGIN_ARRAY:
                List<Object> list = new ArrayList<>();
                in.beginArray();

                while (in.hasNext()) {
                    list.add(this.read(in));
                }

                in.endArray();
                return list;
            case BEGIN_OBJECT:
                final JsonObject o = gson.fromJson(in, TypeToken.get(JsonObject.class).getType());
                if (o.has(TYPE_FIELD_NAME)) {
                    return gson.fromJson(o, TypeToken.get(toClass(o.get(TYPE_FIELD_NAME).getAsString())).getType());
                } else {
                    try (final JsonReader jsonReader = gson.newJsonReader(new StringReader(o.getAsString()))) {
                        Map<String, Object> map = new LinkedTreeMap<>();
                        jsonReader.beginObject();

                        while (jsonReader.hasNext()) {
                            map.put(jsonReader.nextName(), this.read(jsonReader));
                        }

                        jsonReader.endObject();
                        return map;
                    }
                }
            case STRING:
                return in.nextString();
            case NUMBER:
                return in.nextDouble();
            case BOOLEAN:
                return in.nextBoolean();
            case NULL:
                in.nextNull();
                return null;
            default:
                throw new IllegalStateException();
        }
    }

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
}
