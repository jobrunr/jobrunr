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
}