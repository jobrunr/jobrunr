package org.jobrunr.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.jobrunr.configuration.JobRunr;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JarUtilsTest {

    @Test
    void jobRunrVersion() {
        assertThat(JarUtils.getVersion(JobRunr.class))
                .satisfiesAnyOf(
                        val -> assertThat(val).isEqualTo("1.0.0-SNAPSHOT"),
                        val -> assertThat(val).matches("(\\d)+.(\\d)+.(\\d)+(-.*)?")
                );
    }

    @Test
    void gsonVersion() {
        assertThat(JarUtils.getVersion(Gson.class)).isEqualTo("2.12.1");
    }

    @Test
    void jacksonVersion() {
        assertThat(JarUtils.getVersion(ObjectMapper.class)).isEqualTo("2.19.1");
    }

    @Test
    void testGetManifestAttributeValue() {
        assertThat(JarUtils.getManifestAttributeValue(Gson.class, "Bundle-Developers")).contains("google.com");
    }

}