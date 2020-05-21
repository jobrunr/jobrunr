package org.jobrunr.dashboard.server.http;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.dashboard.server.http.ContentType.APPLICATION_JSON;
import static org.jobrunr.dashboard.server.http.ContentType.APPLICATION_OCTET_STREAM;
import static org.jobrunr.dashboard.server.http.ContentType.IMAGE_PNG;
import static org.jobrunr.dashboard.server.http.ContentType.IMAGE_X_ICON;
import static org.jobrunr.dashboard.server.http.ContentType.TEXT_CSS;
import static org.jobrunr.dashboard.server.http.ContentType.TEXT_HTML;
import static org.jobrunr.dashboard.server.http.ContentType.TEXT_JAVASCRIPT;
import static org.jobrunr.dashboard.server.http.ContentType.TEXT_PLAIN;

class ContentTypeTest {

    @Test
    void testHtmlContentType() {
        assertThat(ContentType.from(Path.of("index.html"))).isEqualTo(TEXT_HTML);
    }

    @Test
    void testTextContentType() {
        assertThat(ContentType.from(Path.of("index.txt"))).isEqualTo(TEXT_PLAIN);
    }

    @Test
    void testJsonContentType() {
        assertThat(ContentType.from(Path.of("products.json"))).isEqualTo(APPLICATION_JSON);
    }

    @Test
    void testJavascriptContentType() {
        assertThat(ContentType.from(Path.of("javascript.js"))).isEqualTo(TEXT_JAVASCRIPT);
    }

    @Test
    void testCssContentType() {
        assertThat(ContentType.from(Path.of("stylesheet.css"))).isEqualTo(TEXT_CSS);
    }

    @Test
    void testPngContentType() {
        assertThat(ContentType.from(Path.of("image.png"))).isEqualTo(IMAGE_PNG);
    }

    @Test
    void testIcoContentType() {
        assertThat(ContentType.from(Path.of("image.ico"))).isEqualTo(IMAGE_X_ICON);
    }

    @Test
    void testJavascriptDebugMapsContentType() {
        assertThat(ContentType.from(Path.of("debug.map"))).isEqualTo(APPLICATION_OCTET_STREAM);
    }

    @Test
    void testUnknownContentType() {
        assertThatThrownBy(() -> ContentType.from(Path.of("un.known"))).isInstanceOf(IllegalArgumentException.class);
    }

}