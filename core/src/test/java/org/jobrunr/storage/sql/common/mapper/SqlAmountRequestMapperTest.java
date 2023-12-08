package org.jobrunr.storage.sql.common.mapper;

import org.jobrunr.jobs.Job;
import org.jobrunr.storage.navigation.AmountRequest;
import org.jobrunr.storage.sql.common.db.AnsiDialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnCreatedAt;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnUpdatedAt;

class SqlAmountRequestMapperTest {

    private static final SqlAmountRequestMapper amountMapper = new SqlAmountRequestMapper(new AnsiDialect(), Job.ALLOWED_SORT_COLUMNS.keySet());

    @Test
    void mapUpdatedAt() {
        final AmountRequest pageRequest = ascOnUpdatedAt(2);
        assertThat(amountMapper.orderClause(pageRequest)).isEqualTo(" ORDER BY updatedAt ASC");
    }

    @Test
    void mapCreatedAt() {
        final AmountRequest pageRequest = ascOnCreatedAt(2);
        assertThat(amountMapper.orderClause(pageRequest)).isEqualTo(" ORDER BY createdAt ASC");
    }
    
    @Test
    void mapWithSomeIllegalStuff1() {
        final AmountRequest pageRequest = new AmountRequest("priority:ASC,\"delete * from jobtable\"createdAt:DESC", 2);
        assertThat(amountMapper.orderClause(pageRequest)).isEqualTo(" ORDER BY priority ASC");
    }

    @Test
    void mapWithSomeIllegalStuff2() {
        final AmountRequest pageRequest = new AmountRequest("priority:\"delete * from jobtable\",createdAt:DESC", 2);
        assertThatThrownBy(() -> amountMapper.orderClause(pageRequest))
                .isInstanceOf(IllegalArgumentException.class);
    }
}