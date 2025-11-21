package org.jobrunr.utils.mapper.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.jobrunr.utils.DurationUtils;
import org.jspecify.annotations.Nullable;

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
    public @Nullable Duration read(final JsonReader jsonReader) throws IOException {
        if (jsonReader.peek() == JsonToken.NULL) {
            jsonReader.nextNull();
            return null;
        }

        String durationAsString = jsonReader.nextString();
        if (durationAsString.startsWith("P")) {
            return Duration.parse(durationAsString);
        } else {
            final BigDecimal durationAsSecAndNanoSec = new BigDecimal(durationAsString);
            return DurationUtils.fromBigDecimal(durationAsSecAndNanoSec);
        }
    }
}
