package org.jobrunr.server.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.server.configuration.BackgroundJobServerThreadType.PlatformThreads;
import static org.jobrunr.server.configuration.BackgroundJobServerThreadType.VirtualThreads;
import static org.jobrunr.utils.VersionNumber.v;

class BackgroundJobServerThreadTypeTest {

    @Test
    void platformThreadsAreAlwaysSupported() {
        assertThat(PlatformThreads.isSupported(v("1.8.0-adoptopenjdk"))).isTrue();
        assertThat(PlatformThreads.isSupported(v("1.8.0"))).isTrue();
        assertThat(PlatformThreads.isSupported(v("1.8.0_191"))).isTrue();
        assertThat(PlatformThreads.isSupported(v("1.7.0_232"))).isTrue();
    }

    @Test
    void virtualThreadsAreSupportedAsOfJava21() {
        assertThat(VirtualThreads.isSupported(v("21.0.0-adoptopenjdk"))).isTrue();
        assertThat(VirtualThreads.isSupported(v("21.0.1"))).isTrue();
        assertThat(VirtualThreads.isSupported(v("22.1.0.1.r17"))).isTrue();
        assertThat(VirtualThreads.isSupported(v("23.ea.3"))).isTrue();
        assertThat(VirtualThreads.isSupported(v("17.0.9"))).isFalse();
        assertThat(VirtualThreads.isSupported(v("1.8.9"))).isFalse();
    }

}