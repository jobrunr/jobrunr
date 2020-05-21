package org.jobrunr.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageRequestTest {

    @Test
    void testPageRequest() {
        PageRequest pageRequest = PageRequest.asc(20, 20);

        assertThat(pageRequest.getOffset()).isEqualTo(20);
        assertThat(pageRequest.getLimit()).isEqualTo(20);
    }
}