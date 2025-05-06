package org.jobrunr.utils.mapper.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;

public class DurationAdapter extends TypeAdapter<Duration> {
    @Override
    public void write(final JsonWriter jsonWriter, final Duration duration) throws IOException {
        jsonWriter.value(new BigDecimal(duration.getSeconds() + "." + String.format("%09d", duration.getNano()))); // nanos = 9
    }

    @Override
    public Duration read(final JsonReader jsonReader) throws IOException {
        String durationAsString = jsonReader.nextString();
        if (durationAsString.startsWith("P")) {
            return Duration.parse(durationAsString);
        } else {
            final BigDecimal durationAsSecAndNanoSec = new BigDecimal(durationAsString);
            return Duration.ofSeconds(
                    durationAsSecAndNanoSec.longValue(),
                    durationAsSecAndNanoSec.remainder(BigDecimal.ONE).movePointRight(9).abs().longValue() // nanos = 9
            );
        }
    }
}