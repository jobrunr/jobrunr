package org.jobrunr.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.storage.PageRequest.ascOnUpdatedAt;

class PageRequestTest {

    @Test
    void testPageRequest() {
        PageRequest pageRequest = ascOnUpdatedAt(20, 20);

        assertThat(pageRequest.getOrder()).isEqualTo("updatedAt:ASC");
        assertThat(pageRequest.getOffset()).isEqualTo(20);
        assertThat(pageRequest.getLimit()).isEqualTo(20);
    }

    @Test
    void testNextPage() {
        PageRequest pageRequest = PageRequest.ascOnUpdatedAt( 20);

        PageRequest nextPageRequest = pageRequest.nextPage();
        assertThat(nextPageRequest.getOrder()).isEqualTo("updatedAt:ASC");
        assertThat(nextPageRequest.getOffset()).isEqualTo(20);
        assertThat(nextPageRequest.getLimit()).isEqualTo(20);
    }
}