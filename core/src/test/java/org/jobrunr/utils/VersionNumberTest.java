package org.jobrunr.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.JobRunrAssertions.assertThat;

class VersionNumberTest {

    @Test
    void testVersionNumberConstructor() {
        assertThatCode(() -> new VersionNumber("5.0.0")).doesNotThrowAnyException();
        assertThatCode(() -> new VersionNumber("7.0.0-alpha.1")).doesNotThrowAnyException();
        assertThatCode(() -> new VersionNumber("23.ea.3")).doesNotThrowAnyException();
    }

    @Test
    void testVersionNumber() {
        assertThat(new VersionNumber("6.0.0").isOlderOrEqualTo(new VersionNumber("6.0.0"))).isTrue();
        assertThat(new VersionNumber("5.0.0").isOlderOrEqualTo(new VersionNumber("6.0.0"))).isTrue();
        assertThat(new VersionNumber("9.0.0").isOlderOrEqualTo(new VersionNumber("10.0.0"))).isTrue();
        assertThat(new VersionNumber("1.0.0").isOlderOrEqualTo(new VersionNumber("10.0.0"))).isTrue();
        assertThat(new VersionNumber("5.0.0").isOlderOrEqualTo(new VersionNumber("5.0.1"))).isTrue();
        assertThat(new VersionNumber("6.0.0").isOlderOrEqualTo(new VersionNumber("7.0.0-beta.2"))).isTrue();
        assertThat(new VersionNumber("7.0.0-alpha.1").isOlderOrEqualTo(new VersionNumber("7.0.0-beta.1"))).isTrue();
        assertThat(new VersionNumber("7.0.0-beta.2").isOlderOrEqualTo(new VersionNumber("7.0.0-beta.3"))).isTrue();
        assertThat(new VersionNumber("6.0.0").isOlderOrEqualTo(new VersionNumber("5.0.1"))).isFalse();
        assertThat(new VersionNumber("10.0.0").isOlderOrEqualTo(new VersionNumber("1.0.0"))).isFalse();
        assertThat(new VersionNumber("10.0.0").isOlderOrEqualTo(new VersionNumber("9.0.0"))).isFalse();
    }

    @Test
    void testVersionNumberIsOlderOrEqualTo() {
        assertThat(VersionNumber.isOlderOrEqualTo("6.0.0", "6.0.0")).isTrue();
        assertThat(VersionNumber.isOlderOrEqualTo("5.0.0", "6.0.0")).isTrue();
        assertThat(VersionNumber.isOlderOrEqualTo("9.0.0", "10.0.0")).isTrue();
        assertThat(VersionNumber.isOlderOrEqualTo("1.0.0", "10.0.0")).isTrue();
        assertThat(VersionNumber.isOlderOrEqualTo("5.0.0", "5.0.1")).isTrue();
        assertThat(VersionNumber.isOlderOrEqualTo("6.0.0", "7.0.0-beta.2")).isTrue();
        assertThat(VersionNumber.isOlderOrEqualTo("7.0.0-alpha.1", "7.0.0-beta.1")).isTrue();
        assertThat(VersionNumber.isOlderOrEqualTo("7.0.0-beta.2", "7.0.0-beta.3")).isTrue();
        assertThat(VersionNumber.isOlderOrEqualTo("6.0.0", "5.0.1")).isFalse();
        assertThat(VersionNumber.isOlderOrEqualTo("10.0.0", "1.0.0")).isFalse();
        assertThat(VersionNumber.isOlderOrEqualTo("10.0.0", "9.0.0")).isFalse();
    }

    @Test
    void testVersionNumberIsOlderThan() {
        assertThat(VersionNumber.isOlderThan("1.8.0_191", "1.8")).isFalse();
        assertThat(VersionNumber.isOlderThan("6.0.0", "6.0.0")).isFalse();
        assertThat(VersionNumber.isOlderThan("5.0.0", "6.0.0")).isTrue();
        assertThat(VersionNumber.isOlderThan("9.0.0", "10.0.0")).isTrue();
        assertThat(VersionNumber.isOlderThan("1.0.0", "10.0.0")).isTrue();
        assertThat(VersionNumber.isOlderThan("5.0.0", "5.0.1")).isTrue();
        assertThat(VersionNumber.isOlderThan("6.0.0", "7.0.0-beta.2")).isTrue();
        assertThat(VersionNumber.isOlderThan("7.0.0-alpha.1", "7.0.0-beta.1")).isTrue();
        assertThat(VersionNumber.isOlderThan("7.0.0-beta.2", "7.0.0-beta.3")).isTrue();
        assertThat(VersionNumber.isOlderThan("6.0.0", "5.0.1")).isFalse();
        assertThat(VersionNumber.isOlderThan("10.0.0", "1.0.0")).isFalse();
        assertThat(VersionNumber.isOlderThan("10.0.0", "9.0.0")).isFalse();
    }

    @Test
    void testVersionNumberIsNewerOrEqualTo() {
        assertThat(VersionNumber.isNewerOrEqualTo("6.0.0", "6.0.0")).isTrue();
        assertThat(VersionNumber.isNewerOrEqualTo("6.0.0", "5.0.0")).isTrue();
        assertThat(VersionNumber.isNewerOrEqualTo("10.0.0", "9.0.0")).isTrue();
        assertThat(VersionNumber.isNewerOrEqualTo("10.0.0", "1.0.0")).isTrue();
        assertThat(VersionNumber.isNewerOrEqualTo("5.0.1", "5.0.0")).isTrue();
        assertThat(VersionNumber.isNewerOrEqualTo("10.6", "10.0.0")).isTrue();
        assertThat(VersionNumber.isNewerOrEqualTo("7.0.0-beta.2", "6.0.0")).isTrue();
        assertThat(VersionNumber.isNewerOrEqualTo("7.0.0-beta.1", "7.0.0-alpha.1")).isTrue();
        assertThat(VersionNumber.isNewerOrEqualTo("7.0.0-beta.3", "7.0.0-beta.2")).isTrue();
        assertThat(VersionNumber.isNewerOrEqualTo("5.0.1", "6.0.0")).isFalse();
        assertThat(VersionNumber.isNewerOrEqualTo("1.0.0", "10.0.0")).isFalse();
        assertThat(VersionNumber.isNewerOrEqualTo("9.0.0", "10.0.0")).isFalse();
        assertThat(VersionNumber.isNewerOrEqualTo("10.6", "11.0.0")).isFalse();
        assertThat(VersionNumber.isNewerOrEqualTo("1.8.0_241", "21")).isFalse();
    }

}