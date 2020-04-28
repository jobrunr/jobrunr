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
        jsonWriter.value(new BigDecimal(duration.getSeconds() + "." + String.format("%09d", duration.getNano())));
    }

    @Override
    public Duration read(final JsonReader jsonReader) throws IOException {
        final BigDecimal durationAsSecAndNanoSec = new BigDecimal(jsonReader.nextString());
        return Duration.ofSeconds(
                durationAsSecAndNanoSec.longValue(),
                durationAsSecAndNanoSec.remainder(BigDecimal.ONE).movePointRight(durationAsSecAndNanoSec.scale()).abs().longValue()
        );
    }
}