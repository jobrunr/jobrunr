package org.jobrunr.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;
import static org.jobrunr.utils.StringUtils.substringAfterLast;
import static org.jobrunr.utils.StringUtils.substringBeforeLast;

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
    void testSubstringBeforeLast() {
        String input = "jar:file:/home/ronald/Projects/Personal/JobRunr/bugs/jobrunr_issue/target/demo-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/jobrunr-1.0.0-SNAPSHOT.jar!/org/jobrunr/storage/sql/common/migrations";
        assertThat(substringBeforeLast(input, "!")).isEqualTo("jar:file:/home/ronald/Projects/Personal/JobRunr/bugs/jobrunr_issue/target/demo-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/jobrunr-1.0.0-SNAPSHOT.jar");
    }

    @Test
    void testSubstringAfterLast() {
        String input = "jar:file:/home/ronald/Projects/Personal/JobRunr/bugs/jobrunr_issue/target/demo-0.0.1-SNAPSHOT.jar!/BOOT-INF/lib/jobrunr-1.0.0-SNAPSHOT.jar!/org/jobrunr/storage/sql/common/migrations";
        assertThat(substringAfterLast(input, "!")).isEqualTo("/org/jobrunr/storage/sql/common/migrations");
    }
}