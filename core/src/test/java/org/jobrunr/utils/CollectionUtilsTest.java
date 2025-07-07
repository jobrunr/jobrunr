package org.jobrunr.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.utils.CollectionUtils.findFirst;
import static org.jobrunr.utils.CollectionUtils.findLast;
import static org.jobrunr.utils.CollectionUtils.getFirst;
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
    void findFirstOfCollection() {
        assertThat(findFirst(null)).isEmpty();
        assertThat(findFirst(Collections.emptyList())).isEmpty();
        assertThat(findFirst(List.of("A"))).isEqualTo(Optional.of("A"));
        assertThat(findFirst(List.of("A", "B"))).isEqualTo(Optional.of("A"));
        assertThat(findFirst(List.of("A", "B", "C"))).isEqualTo(Optional.of("A"));
    }

    @Test
    void getFirstOfCollection() {
        assertThat((Object) getFirst(null)).isNull();
        assertThat((Object) getFirst(Collections.emptyList())).isNull();
        assertThat(getFirst(List.of("A"))).isEqualTo("A");
        assertThat(getFirst(List.of("A", "B"))).isEqualTo("A");
        assertThat(getFirst(List.of("A", "B", "C"))).isEqualTo("A");
    }

    @Test
    void findLastOfCollection() {
        assertThat(findLast(null)).isEmpty();
        assertThat(findLast(Collections.emptyList())).isEmpty();
        assertThat(findLast(List.of("A"))).isEqualTo(Optional.of("A"));
        assertThat(findLast(List.of("A", "B"))).isEqualTo(Optional.of("B"));
        assertThat(findLast(List.of("A", "B", "C"))).isEqualTo(Optional.of("C"));
    }

    @Test
    void getLastOfCollection() {
        assertThat((Object) getLast(null)).isNull();
        assertThat((Object) getLast(Collections.emptyList())).isNull();
        assertThat(getLast(List.of("A"))).isEqualTo("A");
        assertThat(getLast(List.of("A", "B"))).isEqualTo("B");
        assertThat(getLast(List.of("A", "B", "C"))).isEqualTo("C");
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