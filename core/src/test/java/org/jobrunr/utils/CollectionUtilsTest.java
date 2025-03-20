package org.jobrunr.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.utils.CollectionUtils.findLast;
import static org.jobrunr.utils.CollectionUtils.getLast;
import static org.jobrunr.utils.CollectionUtils.isNotNullOrEmpty;
import static org.jobrunr.utils.CollectionUtils.isNullOrEmpty;
import static org.jobrunr.utils.CollectionUtils.mapOf;

class CollectionUtilsTest {

    @Test
    void isNullOrEmptyIsTrueForNullCollection() {
        assertThat(isNullOrEmpty((List) null)).isTrue();
    }

    @Test
    void isNullOrEmptyIsTrueForEmptyCollection() {
        assertThat(isNullOrEmpty(new ArrayList<>())).isTrue();
    }

    @Test
    void isNullOrEmptyIsFalseForNotEmptyCollection() {
        assertThat(isNullOrEmpty(asList("item"))).isFalse();
    }

    @Test
    void isNotNullOrEmptyIsFalseForNullCollection() {
        assertThat(isNotNullOrEmpty((List) null)).isFalse();
    }

    @Test
    void isNotNullOrEmptyIsFalseForEmptyCollection() {
        assertThat(isNotNullOrEmpty(new ArrayList<>())).isFalse();
    }

    @Test
    void isNotNullOrEmptyIsTrueForNotEmptyCollection() {
        assertThat(isNotNullOrEmpty(asList("item"))).isTrue();
    }

    @Test
    void isNullOrEmptyIsFalseForNotEmptyArray() {
        assertThat(isNullOrEmpty(new String[]{"item1", "item2"})).isFalse();
    }

    @Test
    void isNullOrEmptyIsTrueForEmptyArray() {
        assertThat(isNullOrEmpty(new String[]{})).isTrue();
    }

    @Test
    void isNotNullOrEmptyIsFalseForEmptyArray() {
        assertThat(isNotNullOrEmpty(new String[]{})).isFalse();
    }

    @Test
    void testFindLast() {
        assertThat(findLast(null)).isEmpty();
        assertThat(findLast(emptyList())).isEmpty();
        assertThat(findLast(List.of(1, 2))).isEqualTo(Optional.of(2));
    }

    @Test
    void testGetLast() {
        assertThat(Optional.ofNullable(getLast(null))).isEmpty();
        assertThat(Optional.ofNullable(getLast(emptyList()))).isEmpty();
        assertThat(getLast(List.of(1, 2))).isEqualTo(2);
    }

    @Test
    void testMapOf1() {
        final Map<String, String> map = mapOf("key1", "value1");

        assertThat(map).containsEntry("key1", "value1");
    }

    @Test
    void testMapOf2() {
        final Map<String, String> map = mapOf(
                "key1", "value1",
                "key2", "value2"
        );

        assertThat(map)
                .containsEntry("key1", "value1")
                .containsEntry("key2", "value2");
    }
}