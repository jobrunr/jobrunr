package org.jobrunr.utils.mapper.jackson.modules;

import com.fasterxml.jackson.core.JsonGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DurationSerializerTest {

    DurationSerializer durationSerializer = new DurationSerializer();

    @Test
    void testDurationSerializer() throws IOException {
        Duration duration = Duration.ofNanos(323567890098765L);
        JsonGenerator jsonGenerator = mock(JsonGenerator.class);

        durationSerializer.serialize(duration, jsonGenerator, null);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(jsonGenerator).writeNumber(argumentCaptor.capture());

        String captorValue = argumentCaptor.getValue();
        assertThat(captorValue).isEqualTo("323567.890098765");
    }

}