package org.jobrunr.utils.mapper.gson;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.LinkedTreeMap;
import org.jobrunr.utils.reflection.ReflectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static org.jobrunr.utils.mapper.gson.RuntimeClassNameTypeAdapterFactory.TYPE_FIELD_NAME;
import static org.jobrunr.utils.reflection.ReflectionUtils.toClass;

public class GsonJsonElementUtils {

    private GsonJsonElementUtils() {}

    public static boolean isJsonNonNullElement(JsonElement jsonElement) {
        return jsonElement != null && !jsonElement.isJsonNull();
    }

    public static boolean isJsonNullElement(JsonElement jsonElement) {
        return !isJsonNonNullElement(jsonElement);
    }

    public static String getAsStringOrNull(JsonElement jsonElement) {
        if (isJsonNullElement(jsonElement)) return null;
        return jsonElement.getAsString();
    }

    public static Integer getAsIntegerOrNull(JsonElement jsonElement) {
        if (isJsonNullElement(jsonElement)) return null;
        return jsonElement.getAsInt();
    }

    public static Collection<Object> deserializeCollectionElement(JsonElement jsonElement, Class<?> type, BiFunction<Class<?>, JsonElement, Object> deserializer) {
        if (isJsonNullElement(jsonElement)) return null;
        if (!jsonElement.isJsonArray()) throw new JsonParseException("Expected a json array");
        Collection<Object> collection = Set.class.isAssignableFrom(type) ? new LinkedHashSet<>() : new ArrayList<>();
        for (JsonElement el : jsonElement.getAsJsonArray()) {
            collection.add(deserializeElement(el, deserializer));
        }
        return collection;
    }

    public static Map<String, Object> deserializeMapElement(JsonElement jsonElement, Class<?> type, BiFunction<Class<?>, JsonElement, Object> deserializer) {
        if (isJsonNullElement(jsonElement)) return null;
        if (!jsonElement.isJsonObject()) throw new JsonParseException("Expected a json object");
        Map<String, Object> map = ReflectionUtils.cast(ReflectionUtils.newInstance(type));
        for (Map.Entry<String, JsonElement> entry : jsonElement.getAsJsonObject().entrySet()) {
            map.put(entry.getKey(), deserializeElement(entry.getValue(), deserializer));
        }
        return map;
    }

    public static Object deserializeElement(JsonElement element, BiFunction<Class<?>, JsonElement, Object> deserializer) {
        if (isJsonNullElement(element)) return null;
        if (element.isJsonPrimitive()) {
            JsonPrimitive p = element.getAsJsonPrimitive();
            if (p.isBoolean()) return p.getAsBoolean();
            if (p.isNumber()) return p.getAsDouble();
            return p.getAsString();
        }
        if (element.isJsonArray()) {
            return deserializeCollectionElement(element, ArrayList.class, deserializer);
        }
        if (element.isJsonObject()) {
            JsonObject o = element.getAsJsonObject();
            if (o.has(TYPE_FIELD_NAME)) {
                String className = o.remove(TYPE_FIELD_NAME).getAsString();
                Class<?> clazz = toClass(className);
                if (Map.class.isAssignableFrom(clazz)) {
                    return deserializeMapElement(o, clazz, deserializer);
                }
                return deserializer.apply(clazz, o);
            } else {
                return deserializeMapElement(o, LinkedTreeMap.class, deserializer);
            }
        }
        return null;
    }
}
