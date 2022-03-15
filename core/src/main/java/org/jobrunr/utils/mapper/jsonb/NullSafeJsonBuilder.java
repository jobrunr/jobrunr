package org.jobrunr.utils.mapper.jsonb;

import javax.json.*;
import javax.json.bind.Jsonb;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;

public class NullSafeJsonBuilder implements JsonObjectBuilder {

    public static NullSafeJsonBuilder nullSafeJsonObjectBuilder() {
        return new NullSafeJsonBuilder();
    }

    public static NullSafeJsonBuilder nullSafeJsonObjectBuilder(Jsonb jsonb, Object object) {
        final String asJson = jsonb.toJson(object);
        final JsonObject jsonObject = Json.createObjectBuilder(jsonb.fromJson(asJson, JsonObject.class)).build();
        return new NullSafeJsonBuilder(jsonObject);
    }

    public static NullSafeJsonBuilder nullSafeJsonObjectBuilder(JsonObject jsonObject) {
        return new NullSafeJsonBuilder(jsonObject);
    }

    private final JsonObjectBuilder delegate;

    public NullSafeJsonBuilder() {
        delegate = Json.createObjectBuilder();
    }

    public NullSafeJsonBuilder(JsonObject object) {
        delegate = Json.createObjectBuilder(object);
    }

    @Override
    public NullSafeJsonBuilder add(String name, JsonValue value) {
        if (value != null) delegate.add(name, value);
        else delegate.addNull(name);
        return this;
    }

    @Override
    public NullSafeJsonBuilder add(String name, String value) {
        if (value != null) delegate.add(name, value);
        else delegate.addNull(name);
        return this;
    }

    public NullSafeJsonBuilder add(String name, UUID value) {
        if (value != null) delegate.add(name, value.toString());
        else delegate.addNull(name);
        return this;
    }

    @Override
    public NullSafeJsonBuilder add(String name, BigInteger value) {
        if (value != null) delegate.add(name, value);
        else delegate.addNull(name);
        return this;
    }

    @Override
    public NullSafeJsonBuilder add(String name, BigDecimal value) {
        if (value != null) delegate.add(name, value);
        else delegate.addNull(name);

        return this;
    }

    @Override
    public NullSafeJsonBuilder add(String name, int value) {
        delegate.add(name, value);
        return this;
    }

    public NullSafeJsonBuilder add(String name, Long value) {
        if (value != null) add(name, value.longValue());
        else addNull(name);
        return this;
    }

    @Override
    public NullSafeJsonBuilder add(String name, long value) {
        delegate.add(name, value);
        return this;
    }

    @Override
    public NullSafeJsonBuilder add(String name, double value) {
        delegate.add(name, value);
        return this;
    }

    @Override
    public NullSafeJsonBuilder add(String name, boolean value) {
        delegate.add(name, value);
        return this;
    }

    @Override
    public NullSafeJsonBuilder addNull(String name) {
        delegate.addNull(name);
        return this;
    }

    @Override
    public NullSafeJsonBuilder add(String name, JsonObjectBuilder builder) {
        if (builder != null)
            delegate.add(name, builder);
        return this;
    }

    @Override
    public NullSafeJsonBuilder add(String name, JsonArrayBuilder builder) {
        if (builder != null)
            delegate.add(name, builder);
        return this;
    }

    @Override
    public JsonObject build() {
        return delegate.build();
    }
}