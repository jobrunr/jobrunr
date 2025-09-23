package org.jobrunr.utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class NumberUtilsTest {

    @Test
    void isZero() {
        assertThat(NumberUtils.isZero(new BigDecimal("0"))).isTrue();
        assertThat(NumberUtils.isZero(new BigDecimal("0.0"))).isTrue();
        assertThat(NumberUtils.isZero(new BigDecimal("0.00"))).isTrue();

        assertThat(NumberUtils.isZero(new BigDecimal("0.01"))).isFalse();
        assertThat(NumberUtils.isZero(new BigDecimal("1.23"))).isFalse();
        assertThat(NumberUtils.isZero(null)).isFalse();
    }

}