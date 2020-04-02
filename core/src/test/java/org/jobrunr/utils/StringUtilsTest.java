package org.jobrunr.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;

class StringUtilsTest {

    @Test
    public void testIsNullOrEmpty() {
        assertThat(isNullOrEmpty(null)).isTrue();
        assertThat(isNullOrEmpty("")).isTrue();
        assertThat(isNullOrEmpty("bla")).isFalse();
    }

    @Test
    public void testIsNotNullOrEmpty() {
        assertThat(isNotNullOrEmpty(null)).isFalse();
        assertThat(isNotNullOrEmpty("")).isFalse();
        assertThat(isNotNullOrEmpty("bla")).isTrue();
    }

    @Test
    public void testCapitalize() {
        assertThat(StringUtils.capitalize("testMethod")).isEqualTo("TestMethod");
    }

}