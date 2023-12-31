package org.jobrunr.server.configuration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.server.configuration.BackgroundJobServerThreadType.PlatformThreads;
import static org.jobrunr.server.configuration.BackgroundJobServerThreadType.VirtualThreads;
import static org.jobrunr.utils.VersionNumber.isOlderThan;

class BackgroundJobServerThreadTypeTest {

    @Test
    void platformThreadsAreSupportedAsOfJava8() {
        assertThat(isOlderThan("1.8.0", PlatformThreads.getMinimumJavaVersion())).isFalse();
        assertThat(isOlderThan("1.8.0_191", PlatformThreads.getMinimumJavaVersion())).isFalse();
        assertThat(isOlderThan("1.7.0_232", PlatformThreads.getMinimumJavaVersion())).isTrue();
    }

    @Test
    void virtualThreadsAreSupportedAsOfJava21() {
        assertThat(isOlderThan("21.0.1", VirtualThreads.getMinimumJavaVersion())).isFalse();
        assertThat(isOlderThan("22.1.0.1.r17", VirtualThreads.getMinimumJavaVersion())).isFalse();
        assertThat(isOlderThan("23.ea.3", VirtualThreads.getMinimumJavaVersion())).isFalse();
        assertThat(isOlderThan("17.0.9", VirtualThreads.getMinimumJavaVersion())).isTrue();
    }

}