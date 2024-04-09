package org.jobrunr.storage;

import org.jobrunr.storage.navigation.OffsetBasedPageRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_CREATED_AT;

class PageTest {

    @Test
    void testPaging1() {
        Page<String> page = new Page<>(50, new ArrayList<>(), 15, 3,
                new OffsetBasedPageRequest(FIELD_CREATED_AT + ":ASC", 15, 5),
                new OffsetBasedPageRequest(FIELD_CREATED_AT + ":ASC", 10, 5),
                new OffsetBasedPageRequest(FIELD_CREATED_AT + ":ASC", 20, 5));
        assertThat(page.getTotal()).isEqualTo(50);
        assertThat(page.getOffset()).isEqualTo(15);
        assertThat(page.getLimit()).isEqualTo(5);
        assertThat(page.getCurrentPage()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(10);
        assertThat(page.hasPreviousPage()).isTrue();
        assertThat(page.hasNextPage()).isTrue();
    }

    @Test
    void testPaging2() {
        Page<String> page = new Page<>(5, new ArrayList<>(), 2, 1,
                new OffsetBasedPageRequest(FIELD_CREATED_AT + ":ASC", 2, 2),
                new OffsetBasedPageRequest(FIELD_CREATED_AT + ":ASC", 0, 2),
                new OffsetBasedPageRequest(FIELD_CREATED_AT + ":ASC", 4, 2));
        assertThat(page.getTotal()).isEqualTo(5);
        assertThat(page.getOffset()).isEqualTo(2);
        assertThat(page.getLimit()).isEqualTo(2);
        assertThat(page.getCurrentPage()).isEqualTo(1);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.hasPreviousPage()).isTrue();
        assertThat(page.hasNextPage()).isTrue();
    }

    @Test
    void testPaging3() {
        Page<String> page = new Page<>(5, new ArrayList<>(), 0, 0,
                new OffsetBasedPageRequest(FIELD_CREATED_AT + ":ASC", 0, 20),
                null,
                null);
        assertThat(page.getTotal()).isEqualTo(5);
        assertThat(page.getOffset()).isZero();
        assertThat(page.getLimit()).isEqualTo(20);
        assertThat(page.getCurrentPage()).isZero();
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.hasPreviousPage()).isFalse();
        assertThat(page.hasNextPage()).isFalse();
    }

    @Test
    void testPagingStrangeValues1() {
        Page<String> page = new Page<>(5, new ArrayList<>(), 1, 1,
                new OffsetBasedPageRequest(FIELD_CREATED_AT + ":ASC", 1, 4),
                new OffsetBasedPageRequest(FIELD_CREATED_AT + ":ASC", 0, 4),
                null);

        assertThat(page.getTotal()).isEqualTo(5);
        assertThat(page.getOffset()).isEqualTo(1);
        assertThat(page.getLimit()).isEqualTo(4);
        assertThat(page.getCurrentPage()).isEqualTo(1);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.hasPreviousPage()).isTrue();
        assertThat(page.hasNextPage()).isFalse();
    }

    @Test
    void testPagingStrangeValues2() {
        Page<String> page = new Page<>(5, new ArrayList<>(), 1, 1,
                new OffsetBasedPageRequest(FIELD_CREATED_AT + ":ASC", 1, 3),
                new OffsetBasedPageRequest(FIELD_CREATED_AT + ":ASC", 0, 3),
                new OffsetBasedPageRequest(FIELD_CREATED_AT + ":ASC", 3, 3));
        assertThat(page.getTotal()).isEqualTo(5);
        assertThat(page.getOffset()).isEqualTo(1);
        assertThat(page.getLimit()).isEqualTo(3);
        assertThat(page.getCurrentPage()).isEqualTo(1);
        assertThat(page.getTotalPages()).isEqualTo(2); // or 3
        assertThat(page.hasPreviousPage()).isTrue();
        assertThat(page.hasNextPage()).isTrue();
    }
}