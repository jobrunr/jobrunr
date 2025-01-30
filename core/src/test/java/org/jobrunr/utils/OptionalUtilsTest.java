package org.jobrunr.utils;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.utils.OptionalUtils.isNotPresent;
import static org.jobrunr.utils.OptionalUtils.isPresent;


class OptionalUtilsTest {

    @Test
    void testIsPresent() {
        assertThat(isPresent(Optional.ofNullable(null))).isFalse();
        assertThat(isPresent(Optional.ofNullable("present"))).isTrue();
    }

    @Test
    void testIsNotPresent() {
        assertThat(isNotPresent(Optional.ofNullable(null))).isTrue();
        assertThat(isNotPresent(Optional.ofNullable("present"))).isFalse();
    }

}