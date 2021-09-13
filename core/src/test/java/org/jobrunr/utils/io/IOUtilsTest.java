package org.jobrunr.utils.io;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class IOUtilsTest {

    @Test
    void testCopyStreamToWriter() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        StringWriter stringWriter = new StringWriter();
        IOUtils.copyStream(inputStream, stringWriter);

        assertThat(stringWriter.toString()).contains("test");
    }

    @Test
    void testCopyStreamOutputStream() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtils.copyStream(inputStream, outputStream);

        assertThat(outputStream.toString()).contains("test");
    }
}