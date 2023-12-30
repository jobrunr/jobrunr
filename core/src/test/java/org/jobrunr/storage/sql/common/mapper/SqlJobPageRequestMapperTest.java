package org.jobrunr.storage.sql.common.mapper;

import org.jobrunr.storage.Paging;
import org.jobrunr.storage.navigation.AmountRequest;
import org.jobrunr.storage.navigation.OffsetBasedPageRequest;
import org.jobrunr.storage.sql.common.JobTable;
import org.jobrunr.storage.sql.common.db.AnsiDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.storage.Paging.OffsetBasedPage.ascOnUpdatedAt;

@ExtendWith(MockitoExtension.class)
class SqlJobPageRequestMapperTest {

    @Mock
    private JobTable jobTable;

    private SqlJobPageRequestMapper jobPageRequestMapper;

    @BeforeEach
    void setUp() {
        jobPageRequestMapper = new SqlJobPageRequestMapper(jobTable, new AnsiDialect());
    }

    @Test
    void sqlJobPageRequestMapperMapsOffsetBasedPageRequest() {
        OffsetBasedPageRequest offsetBasedPageRequest = ascOnUpdatedAt(20, 10);

        String filter = jobPageRequestMapper.map(offsetBasedPageRequest);
        assertThat(filter).isEqualTo(" ORDER BY updatedAt ASC LIMIT :limit OFFSET :offset");
    }

    @Test
    void sqlJobPageRequestMapperMapsOffsetBasedPageRequestSwitchesToAmountIfOffsetIs0() {
        OffsetBasedPageRequest offsetBasedPageRequest = ascOnUpdatedAt(0, 10);

        String filter = jobPageRequestMapper.map(offsetBasedPageRequest);
        assertThat(filter).isEqualTo(" ORDER BY updatedAt ASC LIMIT :limit");
    }

    @Test
    void sqlJobPageRequestMapperMapsAmountBasedPageRequest() {
        AmountRequest amountRequest = Paging.AmountBasedList.ascOnUpdatedAt(10);

        String filter = jobPageRequestMapper.map(amountRequest);
        assertThat(filter).isEqualTo(" ORDER BY updatedAt ASC LIMIT :limit");
    }
}