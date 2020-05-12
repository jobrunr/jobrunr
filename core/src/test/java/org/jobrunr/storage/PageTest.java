package org.jobrunr.storage;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class PageTest {

    @Test
    public void bla() {
        assertThat(getBucket(1)).isEqualTo(1);
        assertThat(getBucket(2)).isEqualTo(2);
        assertThat(getBucket(3)).isEqualTo(3);
        assertThat(getBucket(4)).isEqualTo(1);
        assertThat(getBucket(5)).isEqualTo(2);
        assertThat(getBucket(6)).isEqualTo(3);
        assertThat(getBucket(7)).isEqualTo(1);
        assertThat(getBucket(8)).isEqualTo(2);
        assertThat(getBucket(9)).isEqualTo(3);
        assertThat(getBucket(10)).isEqualTo(1);
        assertThat(getBucket(11)).isEqualTo(2);
        assertThat(getBucket(12)).isEqualTo(3);
        assertThat(getBucket(13)).isEqualTo(1);
        assertThat(getBucket(14)).isEqualTo(2);
        assertThat(getBucket(15)).isEqualTo(3);
        assertThat(getBucket(16)).isEqualTo(1);
        assertThat(getBucket(17)).isEqualTo(2);
        assertThat(getBucket(18)).isEqualTo(3);
        assertThat(getBucket(19)).isEqualTo(1);
        assertThat(getBucket(20)).isEqualTo(2);
        assertThat(getBucket(21)).isEqualTo(3);
        assertThat(getBucket(22)).isEqualTo(1);
        assertThat(getBucket(23)).isEqualTo(2);
        assertThat(getBucket(24)).isEqualTo(3);
    }

    private int getBucket(int nbr) {
        int bucket = nbr % 3;
        if (bucket == 0) bucket = 3;
        return bucket;
    }

    @Test
    public void testPaging1() {
        Page<String> page = new Page<>(50, new ArrayList<>(), 15, 5);
        assertThat(page.getTotal()).isEqualTo(50);
        assertThat(page.getOffset()).isEqualTo(15);
        assertThat(page.getLimit()).isEqualTo(5);
        assertThat(page.getCurrentPage()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(10);
        assertThat(page.hasPrevious()).isEqualTo(true);
        assertThat(page.hasNext()).isEqualTo(true);
    }

    @Test
    public void testPaging2() {
        Page<String> page = new Page<>(5, new ArrayList<>(), 2, 2);
        assertThat(page.getTotal()).isEqualTo(5);
        assertThat(page.getOffset()).isEqualTo(2);
        assertThat(page.getLimit()).isEqualTo(2);
        assertThat(page.getCurrentPage()).isEqualTo(1);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.hasPrevious()).isEqualTo(true);
        assertThat(page.hasNext()).isEqualTo(true);
    }

    @Test
    public void testPaging3() {
        Page<String> page = new Page<>(5, new ArrayList<>(), 0, 20);
        assertThat(page.getTotal()).isEqualTo(5);
        assertThat(page.getOffset()).isEqualTo(0);
        assertThat(page.getLimit()).isEqualTo(20);
        assertThat(page.getCurrentPage()).isEqualTo(0);
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.hasPrevious()).isEqualTo(false);
        assertThat(page.hasNext()).isEqualTo(false);
    }

    @Test
    public void testPagingStrangeValues1() {
        Page<String> page = new Page<>(5, new ArrayList<>(), 1, 4);
        assertThat(page.getTotal()).isEqualTo(5);
        assertThat(page.getOffset()).isEqualTo(1);
        assertThat(page.getLimit()).isEqualTo(4);
        assertThat(page.getCurrentPage()).isEqualTo(1);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.hasPrevious()).isEqualTo(true);
        assertThat(page.hasNext()).isEqualTo(false);
    }

    @Test
    public void testPagingStrangeValues2() {
        Page<String> page = new Page<>(5, new ArrayList<>(), 1, 3);
        assertThat(page.getTotal()).isEqualTo(5);
        assertThat(page.getOffset()).isEqualTo(1);
        assertThat(page.getLimit()).isEqualTo(3);
        assertThat(page.getCurrentPage()).isEqualTo(1);
        assertThat(page.getTotalPages()).isEqualTo(2); // or 3
        assertThat(page.hasPrevious()).isEqualTo(true);
        assertThat(page.hasNext()).isEqualTo(true);
    }

}