package org.jobrunr.utils.mapper.jsonb;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue.ValueType;

public class JsonbUtils {

    private JsonbUtils() {

    }

    public static Integer getIntegerOrNull(JsonObject jsonObject, String key) {
        if (jsonObject.containsKey(key) && !jsonObject.isNull(key)) {
            ValueType valueType = jsonObject.get(key).getValueType();
            if (ValueType.NUMBER == valueType) {
                return jsonObject.getInt(key);
            } else if (ValueType.STRING == valueType) {
                return Integer.valueOf(jsonObject.getString(key));
            }
        }
        return null;
    }
}
