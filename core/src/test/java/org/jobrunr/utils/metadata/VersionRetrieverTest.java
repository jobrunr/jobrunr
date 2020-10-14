package org.jobrunr.utils.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.jobrunr.configuration.JobRunr;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VersionRetrieverTest {

    @Test
    void jobRunrVersion() {
        assertThat(VersionRetriever.getVersion(JobRunr.class)).isEqualTo("Unable to determine version");
    }

    @Test
    void gsonVersion() {
        assertThat(VersionRetriever.getVersion(Gson.class)).isEqualTo("2.8.6");
    }

    @Test
    void jacksonVersion() {
        assertThat(VersionRetriever.getVersion(ObjectMapper.class)).isEqualTo("2.11.3");
    }

}