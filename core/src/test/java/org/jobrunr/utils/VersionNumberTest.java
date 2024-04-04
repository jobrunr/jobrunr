package org.jobrunr.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.utils.VersionNumber.v;

class VersionNumberTest {

    @Test
    void testVersionNumberConstructor() {
        assertThatCode(() -> v("5.0.0")).doesNotThrowAnyException();
        assertThatCode(() -> v("7.0.0-alpha.1")).doesNotThrowAnyException();
        assertThatCode(() -> v("23.ea.3")).doesNotThrowAnyException();
        assertThatCode(() -> v("21-ea")).doesNotThrowAnyException();
        assertThatCode(() -> v("this is even not parseable as a version")).doesNotThrowAnyException();
    }

    @Test
    void testMethods() {
        VersionNumber v = v("1.2.3-alpha.6");
        assertThat(v.getMajorVersion()).isEqualTo("1");
        assertThat(v.getMinorVersion()).isEqualTo("2");
        assertThat(v.getPatchVersion()).isEqualTo("3");
    }

    @Test
    void testIsOlderOrEqualTo() {
        assertThat(v("6.0.0").isOlderOrEqualTo(v("6.0.0"))).isTrue();
        assertThat(v("5.0.0").isOlderOrEqualTo(v("6.0.0"))).isTrue();
        assertThat(v("9.0.0").isOlderOrEqualTo(v("10.0.0"))).isTrue();
        assertThat(v("1.0.0").isOlderOrEqualTo(v("10.0.0"))).isTrue();
        assertThat(v("5.0.0").isOlderOrEqualTo(v("5.0.1"))).isTrue();
        assertThat(v("6.0.0").isOlderOrEqualTo(v("7.0.0-beta.2"))).isTrue();
        assertThat(v("7.0.0-alpha.1").isOlderOrEqualTo(v("7.0.0-beta.1"))).isTrue();
        assertThat(v("7.0.0-beta.2").isOlderOrEqualTo(v("7.0.0-beta.3"))).isTrue();

        assertThat(v("6.0.0").isOlderOrEqualTo(v("5.0.1"))).isFalse();
        assertThat(v("10.0.0").isOlderOrEqualTo(v("1.0.0"))).isFalse();
        assertThat(v("10.0.0").isOlderOrEqualTo(v("9.0.0"))).isFalse();
        assertThat(v("10.0.0").isOlderOrEqualTo(v("9.0.0"))).isFalse();
    }

    @Test
    void testIsOlderThan() {
        assertThat(v("5.0.0").isOlderThan(v("6.0.0"))).isTrue();
        assertThat(v("9.0.0").isOlderThan(v("10.0.0"))).isTrue();
        assertThat(v("1.0.0").isOlderThan(v("10.0.0"))).isTrue();
        assertThat(v("5.0.0").isOlderThan(v("5.0.1"))).isTrue();
        assertThat(v("6.0.0").isOlderThan(v("7.0.0-beta.2"))).isTrue();
        assertThat(v("7.0.0-alpha.1").isOlderThan(v("7.0.0-beta.1"))).isTrue();
        assertThat(v("7.0.0-beta.2").isOlderThan(v("7.0.0-beta.3"))).isTrue();

        assertThat(v("1.8.0_191").isOlderThan(v("1.8"))).isFalse();
        assertThat(v("6.0.0").isOlderThan(v("6.0.0"))).isFalse();
        assertThat(v("6.0.0").isOlderThan(v("5.0.1"))).isFalse();
        assertThat(v("10.0.0").isOlderThan(v("1.0.0"))).isFalse();
        assertThat(v("10.0.0").isOlderThan(v("9.0.0"))).isFalse();
    }

    @Test
    void testIsNewerOrEqualTo() {
        assertThat(v("6.0.0").isNewerOrEqualTo(v("6.0.0"))).isTrue();
        assertThat(v("6.0.0").isNewerOrEqualTo(v("5.0.0"))).isTrue();
        assertThat(v("10.0.0").isNewerOrEqualTo(v("9.0.0"))).isTrue();
        assertThat(v("10.0.0").isNewerOrEqualTo(v("1.0.0"))).isTrue();
        assertThat(v("5.0.1").isNewerOrEqualTo(v("5.0.0"))).isTrue();
        assertThat(v("10.6").isNewerOrEqualTo(v("10.0.0"))).isTrue();
        assertThat(v("7.0.0-beta.2").isNewerOrEqualTo(v("6.0.0"))).isTrue();
        assertThat(v("7.0.0-beta.1").isNewerOrEqualTo(v("7.0.0-alpha.1"))).isTrue();
        assertThat(v("7.0.0-beta.3").isNewerOrEqualTo(v("7.0.0-beta.2"))).isTrue();

        assertThat(v("5.0.1").isNewerOrEqualTo(v("6.0.0"))).isFalse();
        assertThat(v("1.0.0").isNewerOrEqualTo(v("10.0.0"))).isFalse();
        assertThat(v("9.0.0").isNewerOrEqualTo(v("10.0.0"))).isFalse();
        assertThat(v("10.6").isNewerOrEqualTo(v("11.0.0"))).isFalse();
        assertThat(v("1.8.0_241").isNewerOrEqualTo(v("21"))).isFalse();
    }

    @Test
    void hasMajorVersionHigherOrEqualTo() {
        assertThat(v("21.0.0-adoptopenjdk").hasMajorVersionHigherOrEqualTo(21)).isTrue();

        assertThat(v("6.0.0").hasMajorVersionHigherOrEqualTo(v("6.0.0"))).isTrue();
        assertThat(v("6.0.0").hasMajorVersionHigherOrEqualTo(v("5.0.0"))).isTrue();
        assertThat(v("10.0.0").hasMajorVersionHigherOrEqualTo(v("9.0.0"))).isTrue();
        assertThat(v("10.0.0").hasMajorVersionHigherOrEqualTo(v("1.0.0"))).isTrue();
        assertThat(v("5.0.1").hasMajorVersionHigherOrEqualTo(v("5.0.0"))).isTrue();
        assertThat(v("10.6").hasMajorVersionHigherOrEqualTo(v("10.0.0"))).isTrue();
        assertThat(v("7.0.0-beta.2").hasMajorVersionHigherOrEqualTo(v("6.0.0"))).isTrue();
        assertThat(v("7.0.0-beta.1").hasMajorVersionHigherOrEqualTo(v("7.0.0-alpha.1"))).isTrue();
        assertThat(v("7.0.0-beta.3").hasMajorVersionHigherOrEqualTo(v("7.0.0-beta.2"))).isTrue();

        assertThat(v("5.0.1").hasMajorVersionHigherOrEqualTo(v("6.0.0"))).isFalse();
        assertThat(v("1.0.0").hasMajorVersionHigherOrEqualTo(v("10.0.0"))).isFalse();
        assertThat(v("9.0.0").hasMajorVersionHigherOrEqualTo(v("10.0.0"))).isFalse();
        assertThat(v("10.6").hasMajorVersionHigherOrEqualTo(v("11.0.0"))).isFalse();
        assertThat(v("1.8.0_241").hasMajorVersionHigherOrEqualTo(v("21"))).isFalse();
    }

    @Test
    void hasMajorAndMinorVersionHigherOrEqualTo() {
        assertThat(v("6.0.0").hasMajorAndMinorVersionHigherOrEqualTo(v("6.0.0"))).isTrue();
        assertThat(v("6.1.1").hasMajorAndMinorVersionHigherOrEqualTo(v("6.1.0"))).isTrue();
        assertThat(v("6.0.0").hasMajorAndMinorVersionHigherOrEqualTo(v("5.0.0"))).isTrue();
        assertThat(v("10.0.0").hasMajorAndMinorVersionHigherOrEqualTo(v("9.0.0"))).isTrue();
        assertThat(v("10.0.0").hasMajorAndMinorVersionHigherOrEqualTo(v("1.0.0"))).isTrue();
        assertThat(v("5.0.1").hasMajorAndMinorVersionHigherOrEqualTo(v("5.0.0"))).isTrue();
        assertThat(v("10.6").hasMajorAndMinorVersionHigherOrEqualTo(v("10.0.0"))).isTrue();
        assertThat(v("7.0.0-beta.2").hasMajorAndMinorVersionHigherOrEqualTo(v("6.0.0"))).isTrue();
        assertThat(v("7.0.0-beta.1").hasMajorAndMinorVersionHigherOrEqualTo(v("7.0.0-alpha.1"))).isTrue();
        assertThat(v("7.0.0-beta.3").hasMajorAndMinorVersionHigherOrEqualTo(v("7.0.0-beta.2"))).isTrue();

        assertThat(v("6.0.0").hasMajorAndMinorVersionHigherOrEqualTo(v("6.1.0"))).isFalse();
        assertThat(v("5.0.1").hasMajorAndMinorVersionHigherOrEqualTo(v("6.0.0"))).isFalse();
        assertThat(v("1.0.0").hasMajorAndMinorVersionHigherOrEqualTo(v("10.0.0"))).isFalse();
        assertThat(v("9.0.0").hasMajorAndMinorVersionHigherOrEqualTo(v("10.0.0"))).isFalse();
        assertThat(v("10.6").hasMajorAndMinorVersionHigherOrEqualTo(v("11.0.0"))).isFalse();
        assertThat(v("1.8.0_241").hasMajorAndMinorVersionHigherOrEqualTo(v("21"))).isFalse();

        assertThat(v("8.3.0").hasMajorMinorAndPatchVersionHigherOrEqualTo("8.0.1")).isTrue();
    }
}