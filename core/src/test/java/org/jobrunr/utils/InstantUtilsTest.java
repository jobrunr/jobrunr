package org.jobrunr.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.utils.InstantUtils.isInstantAfterOrEqualTo;
import static org.jobrunr.utils.InstantUtils.isInstantBeforeOrEqualTo;
import static org.jobrunr.utils.InstantUtils.isInstantInPeriod;

class InstantUtilsTest {

    @Test
    void testIsInstantInPeriod() {
        Instant now = Instant.now();
        Instant startPeriod = now.minusSeconds(10);
        Instant endPeriod = now.plusSeconds(10);
        
        assertThat(isInstantInPeriod(now, startPeriod, endPeriod)).isTrue();

        assertThat(isInstantInPeriod(now.minusSeconds(20), startPeriod, endPeriod)).isFalse();
        assertThat(isInstantInPeriod(now.plusSeconds(20), startPeriod, endPeriod)).isFalse();
    }

    @Test
    void testIsInstantBeforeOrEqualTo() {
        Instant now = Instant.now();
        assertThat(isInstantBeforeOrEqualTo(now, now)).isTrue();
        assertThat(isInstantBeforeOrEqualTo(now, now.plusSeconds(1))).isTrue();

        assertThat(isInstantBeforeOrEqualTo(now, now.minusSeconds(1))).isFalse();
    }

    @Test
    void testIsInstantAfterOrEqualTo() {
        Instant now = Instant.now();
        assertThat(isInstantAfterOrEqualTo(now, now)).isTrue();
        assertThat(isInstantAfterOrEqualTo(now, now.minusSeconds(1))).isTrue();

        assertThat(isInstantAfterOrEqualTo(now, now.plusSeconds(1))).isFalse();
    }
}