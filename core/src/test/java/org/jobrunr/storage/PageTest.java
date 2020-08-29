package org.jobrunr.storage;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class PageTest {

    @Test
    void testPaging1() {
        Page<String> page = new Page<>(50, new ArrayList<>(), 15, 5);
        assertThat(page.getTotal()).isEqualTo(50);
        assertThat(page.getOffset()).isEqualTo(15);
        assertThat(page.getLimit()).isEqualTo(5);
        assertThat(page.getCurrentPage()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(10);
        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void testPaging2() {
        Page<String> page = new Page<>(5, new ArrayList<>(), 2, 2);
        assertThat(page.getTotal()).isEqualTo(5);
        assertThat(page.getOffset()).isEqualTo(2);
        assertThat(page.getLimit()).isEqualTo(2);
        assertThat(page.getCurrentPage()).isEqualTo(1);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    void testPaging3() {
        Page<String> page = new Page<>(5, new ArrayList<>(), 0, 20);
        assertThat(page.getTotal()).isEqualTo(5);
        assertThat(page.getOffset()).isZero();
        assertThat(page.getLimit()).isEqualTo(20);
        assertThat(page.getCurrentPage()).isZero();
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.hasPrevious()).isFalse();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void testPagingStrangeValues1() {
        Page<String> page = new Page<>(5, new ArrayList<>(), 1, 4);
        assertThat(page.getTotal()).isEqualTo(5);
        assertThat(page.getOffset()).isEqualTo(1);
        assertThat(page.getLimit()).isEqualTo(4);
        assertThat(page.getCurrentPage()).isEqualTo(1);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void testPagingStrangeValues2() {
        Page<String> page = new Page<>(5, new ArrayList<>(), 1, 3);
        assertThat(page.getTotal()).isEqualTo(5);
        assertThat(page.getOffset()).isEqualTo(1);
        assertThat(page.getLimit()).isEqualTo(3);
        assertThat(page.getCurrentPage()).isEqualTo(1);
        assertThat(page.getTotalPages()).isEqualTo(2); // or 3
        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.hasNext()).isTrue();
    }

}