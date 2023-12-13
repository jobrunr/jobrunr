package org.jobrunr.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.utils.StringUtils.*;

class StringUtilsTest {

    @Test
    void testIsNullOrEmpty() {
        assertThat(isNullOrEmpty(null)).isTrue();
        assertThat(isNullOrEmpty("")).isTrue();
        assertThat(isNullOrEmpty("bla")).isFalse();
    }

    @Test
    void testIsNotNullOrEmpty() {
        assertThat(isNotNullOrEmpty(null)).isFalse();
        assertThat(isNotNullOrEmpty("")).isFalse();
        assertThat(isNotNullOrEmpty("bla")).isTrue();
    }

    @Test
    void testCapitalize() {
        assertThat(StringUtils.capitalize("testMethod")).isEqualTo("TestMethod");
    }

    @Test
    void testSubstringBeforeSplitterSingleChar() {
        assertThat(substringBefore("15", "-")).isEqualTo("15");
        assertThat(substringBefore("15-ea", "-")).isEqualTo("15");
    }

    @Test
    void testSubstringBeforeSplitterMultiChar() {
        assertThat(substringBefore("this is a test", " is ")).isEqualTo("this");
        assertThat(substringBefore("this is a test", " was ")).isEqualTo("this is a test");
    }

    @Test
    void testSubstringBeforeLast() {
        String input = "jar:file:/home/ronald/Projects/Personal/JobRunr/bugs/jobrunr_issue/target/demo-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/jobrunr-1.0.0-SNAPSHOT.jar!/org/jobrunr/storage/sql/common/migrations";
        assertThat(substringBeforeLast(input, "!")).isEqualTo("jar:file:/home/ronald/Projects/Personal/JobRunr/bugs/jobrunr_issue/target/demo-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/jobrunr-1.0.0-SNAPSHOT.jar");
    }

    @Test
    void testSubstringAfterSplitterSingleChar() {
        assertThat(substringAfter("15", "-")).isNull();
        assertThat(substringAfter("15-ea", "-")).isEqualTo("ea");
    }

    @Test
    void testSubstringAfterSplitterMultiChar() {
        assertThat(substringAfter("this is a test", " is ")).isEqualTo("a test");
        assertThat(substringAfter("this is a test", " was ")).isNull();
    }

    @Test
    void testSubstringAfterLast() {
        String input = "jar:file:/home/ronald/Projects/Personal/JobRunr/bugs/jobrunr_issue/target/demo-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/jobrunr-1.0.0-SNAPSHOT.jar!/org/jobrunr/storage/sql/common/migrations";
        assertThat(substringAfterLast(input, "!")).isEqualTo("/org/jobrunr/storage/sql/common/migrations");
    }

    @Test
    void testSubstringBetween() {
        assertThat(substringBetween("${some.string}", "${", "}")).isEqualTo("some.string");
        assertThat(substringBetween("some.string", "${", "}")).isNull();
    }

    @Test
    void testLenientSubstringBetween() {
        assertThat(lenientSubstringBetween("open=some.string&close", "open=", "&close")).isEqualTo("some.string");
        assertThat(lenientSubstringBetween("open=some.string", "open=", "&close")).isEqualTo("some.string");
        assertThat(lenientSubstringBetween(null, "open=", "&close")).isNull();
    }
}