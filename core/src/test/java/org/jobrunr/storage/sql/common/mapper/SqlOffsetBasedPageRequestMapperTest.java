package org.jobrunr.storage.sql.common.mapper;

import org.jobrunr.jobs.Job;
import org.jobrunr.storage.Paging;
import org.jobrunr.storage.navigation.OffsetBasedPageRequest;
import org.jobrunr.storage.sql.common.JobTable;
import org.jobrunr.storage.sql.common.db.AnsiDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SqlOffsetBasedPageRequestMapperTest {

    @Mock
    private JobTable jobTable;

    private SqlOffsetBasedPageRequestMapper offsetBasedPageRequestMapper;

    @BeforeEach
    void setUp() {
        offsetBasedPageRequestMapper = new SqlOffsetBasedPageRequestMapper(new AnsiDialect(), Job.ALLOWED_SORT_COLUMNS.keySet());
    }

    @Test
    void sqlOffsetBasedPageRequestMapperMapsOrder() {
        OffsetBasedPageRequest offsetBasedPageRequest = Paging.OffsetBasedPage.ascOnScheduledAt(10);

        String filter = offsetBasedPageRequestMapper.mapToSqlQuery(offsetBasedPageRequest, jobTable);

        verify(jobTable).with("offset", 0L);
        verify(jobTable).with("limit", 10);
        assertThat(filter).isEqualTo(" ORDER BY scheduledAt ASC LIMIT :limit OFFSET :offset");
    }
}