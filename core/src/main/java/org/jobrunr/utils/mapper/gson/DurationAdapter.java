package org.jobrunr.utils.mapper.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.jobrunr.utils.DurationUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;

public class DurationAdapter extends TypeAdapter<Duration> {
    @Override
    public void write(final JsonWriter jsonWriter, final Duration duration) throws IOException {
        if (duration == null) {
            jsonWriter.nullValue();
        } else {
            jsonWriter.value(DurationUtils.toBigDecimal(duration));
        }
    }

    @Override
    public Duration read(final JsonReader jsonReader) throws IOException {
        if (jsonReader.peek() == JsonToken.NULL) {
            jsonReader.nextNull();
            return null;
        }

        final BigDecimal durationAsSecAndNanoSec = new BigDecimal(jsonReader.nextString());
        return DurationUtils.fromBigDecimal(durationAsSecAndNanoSec);
    }
}