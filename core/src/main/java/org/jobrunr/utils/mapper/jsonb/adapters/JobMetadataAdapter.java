package org.jobrunr.utils.mapper.jsonb.adapters;

import org.jobrunr.utils.mapper.jsonb.JobRunrJsonb;

import javax.json.*;
import javax.json.bind.adapter.JsonbAdapter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static javax.json.JsonValue.ValueType.*;
import static org.jobrunr.utils.mapper.jsonb.NullSafeJsonBuilder.nullSafeJsonObjectBuilder;
import static org.jobrunr.utils.reflection.ReflectionUtils.toClass;

public class JobMetadataAdapter implements JsonbAdapter<Map<String, Object>, JsonObject> {

    private final JobRunrJsonb jsonb;

    public JobMetadataAdapter(JobRunrJsonb jsonb) {
        this.jsonb = jsonb;
    }

    @Override
    public JsonObject adaptToJson(Map<String, Object> map) throws Exception {
        final JsonObjectBuilder metadataJsonObjectBuilder = Json.createObjectBuilder()
                .add("@class", "java.util.concurrent.ConcurrentHashMap");

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object object = entry.getValue();
            final JsonValue jsonValue = this.jsonb.fromJsonToJsonValue(object);
            if (OBJECT.equals(jsonValue.getValueType())) {
                final JsonObjectBuilder childObjectBuilder = nullSafeJsonObjectBuilder((JsonObject) jsonValue)
                        .add("@class", object.getClass().getName());
                metadataJsonObjectBuilder.add(key, childObjectBuilder);
            } else if (ARRAY.equals(jsonValue.getValueType())) {
                throw new UnsupportedOperationException("Not supported: " + jsonValue.getValueType());
            } else {
                metadataJsonObjectBuilder.add(key, jsonValue);
            }
        }
        return metadataJsonObjectBuilder.build();
    }

    @Override
    public ConcurrentHashMap<String, Object> adaptFromJson(JsonObject jsonMetadataObject) throws Exception {
        final ConcurrentHashMap<String, Object> result = new ConcurrentHashMap<>();
        for (Map.Entry<String, JsonValue> entry : jsonMetadataObject.entrySet()) {
            String key = entry.getKey();
            if ("@class".equals(key)) continue;

            final JsonValue jsonValue = entry.getValue();
            if (OBJECT.equals(jsonValue.getValueType())) {
                final JsonObject jsonObject = jsonValue.asJsonObject();
                final Object o = jsonb.fromJsonValue(jsonObject, toClass(jsonObject.getString("@class")));
                result.put(key, o);
            } else if (STRING.equals(jsonValue.getValueType())) {
                result.put(key, ((JsonString) jsonValue).getString());
            } else if (NUMBER.equals(jsonValue.getValueType())) {
                Number number = ((JsonNumber) jsonValue).numberValue();
                result.put(key, number.doubleValue());
            } else if (TRUE.equals(jsonValue.getValueType())) {
                result.put(key, true);
            } else if (FALSE.equals(jsonValue.getValueType())) {
                result.put(key, false);
            }
        }
        return result;
    }
}
