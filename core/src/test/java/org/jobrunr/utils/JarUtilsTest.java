package org.jobrunr.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.assertj.core.api.Assertions;
import org.jobrunr.configuration.JobRunr;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JarUtilsTest {

    @Test
    void jobRunrVersion() {
        Assertions.assertThat(JarUtils.getVersion(JobRunr.class))
                .satisfiesAnyOf(
                        val -> assertThat(val).isEqualTo("1.0.0-SNAPSHOT"),
                        val -> assertThat(val).matches("(\\d)+.(\\d)+.(\\d)+(-.*)?")
                );
    }

    @Test
    void gsonVersion() {
        assertThat(JarUtils.getVersion(Gson.class)).isEqualTo("2.10.1");
    }

    @Test
    void jacksonVersion() {
        assertThat(JarUtils.getVersion(ObjectMapper.class)).isEqualTo("2.17.0");
    }

    @Test
    void testGetManifestAttributeValue() {
        assertThat(JarUtils.getManifestAttributeValue(Gson.class, "Bundle-Developers")).contains("google.com");
    }

}