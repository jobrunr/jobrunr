package org.jobrunr.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.utils.InstantUtils.isInstantAfterOrEqualToOther;
import static org.jobrunr.utils.InstantUtils.isInstantBeforeOrEqualToOther;

class InstantUtilsTest {

    @Test
    void testIsInstantBeforeOrEqualToOther() {
        Instant now = Instant.now();
        assertThat(isInstantBeforeOrEqualToOther(now, now)).isTrue();
        assertThat(isInstantBeforeOrEqualToOther(now, now.plusSeconds(1))).isTrue();

        assertThat(isInstantBeforeOrEqualToOther(now, now.minusSeconds(1))).isFalse();
    }

    @Test
    void testIsInstantAfterOrEqualToOther() {
        Instant now = Instant.now();
        assertThat(isInstantAfterOrEqualToOther(now, now)).isTrue();
        assertThat(isInstantAfterOrEqualToOther(now, now.minusSeconds(1))).isTrue();

        assertThat(isInstantAfterOrEqualToOther(now, now.plusSeconds(1))).isFalse();
    }
}