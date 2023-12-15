package org.jobrunr.storage.navigation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AmountRequestTest {

    @Test
    void testAmountRequestWithEmptyString() {
        AmountRequest amountRequest = AmountRequest.fromString("");

        assertThat(amountRequest).isNull();
    }

    @Test
    void testOffsetBasedPageRequestWithEmptyString() {
        OffsetBasedPageRequest offsetBasedPageRequest = OffsetBasedPageRequest.fromString("");

        assertThat(offsetBasedPageRequest).isNull();
    }

}