package org.jobrunr.scheduling.carbonaware;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CarbonAwareTest {

    @Test
    void dailyBetween_invalidValuesThrowsException() {
        assertThatCode(() -> CarbonAware.dailyBetween(-1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> CarbonAware.dailyBetween(1, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> CarbonAware.dailyBetween(24, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> CarbonAware.dailyBetween(1, 24)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dailyBetween_crossesOverToNextDate_ThrowsException() {
        assertThatCode(() -> CarbonAware.dailyBetween(11, 11)).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> CarbonAware.dailyBetween(23, 11)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dailyBetween_returnsScheduleStringWithCarbonBounds() {
        assertThat(CarbonAware.dailyBetween(11, 23)).isEqualTo("0 11 * * * [PT0S/PT12H]");
    }

}