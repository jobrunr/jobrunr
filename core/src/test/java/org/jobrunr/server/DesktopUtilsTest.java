package org.jobrunr.server;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DesktopUtilsTest {

    @Test
    void testSupportSystemSleepDetection() {
        boolean supportsSystemSleepDetection = DesktopUtils.systemSupportsSleepDetection();
        if (System.getenv("CI") != null) {
            assertThat(supportsSystemSleepDetection).isFalse();
        } else {
            assertThat(supportsSystemSleepDetection).isTrue();
        }
    }

}