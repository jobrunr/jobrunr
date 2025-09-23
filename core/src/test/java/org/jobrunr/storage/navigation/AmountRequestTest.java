package org.jobrunr.storage.navigation;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AmountRequestTest {

    @Test
    void amountRequestWithEmptyString() {
        assertThat(AmountRequest.fromString("")).isNull();
    }

    @Test
    void getAllOrderTermsForMultipleOrdersButOnlyOneAllowed() {
        var amountRequest = new AmountRequest("updatedAt:ASC,someOtherField:DESC", 10);
        var orderTerms = amountRequest.getAllOrderTerms(Sets.newHashSet("updatedAt"));

        assertThat(orderTerms).hasSize(1);
        var orderTerm = orderTerms.get(0);
        assertThat(orderTerm.getFieldName()).isEqualTo("updatedAt");
        assertThat(orderTerm.getOrder()).isEqualTo(OrderTerm.Order.ASC);
    }

    @Test
    void amountRequestWithEmptyOrderInConstructorDefaultsToAscUpdatedAt() {
        assertThat(new AmountRequest("", 10).getOrder()).isEqualTo("updatedAt:ASC");
    }

    @Test
    void offsetBasedPageRequestWithEmptyString() {
        assertThat(OffsetBasedPageRequest.fromString("")).isNull();
    }

}