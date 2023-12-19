package org.jobrunr.storage.sql.common.mapper;

import org.jobrunr.storage.navigation.AmountRequest;
import org.jobrunr.storage.navigation.OffsetBasedPageRequest;
import org.jobrunr.storage.sql.common.JobTable;
import org.jobrunr.storage.sql.common.db.Dialect;

import static org.jobrunr.jobs.Job.ALLOWED_SORT_COLUMNS;

public class SqlJobPageRequestMapper {

    private final JobTable jobTable;
    private final SqlAmountRequestMapper sqlAmountRequestMapper;
    private final SqlOffsetBasedPageRequestMapper sqlOffsetBasedPageRequestMapper;

    public SqlJobPageRequestMapper(JobTable jobTable, Dialect dialect) {
        this.jobTable = jobTable;
        this.sqlAmountRequestMapper = new SqlAmountRequestMapper(dialect, ALLOWED_SORT_COLUMNS.keySet());
        this.sqlOffsetBasedPageRequestMapper = new SqlOffsetBasedPageRequestMapper(dialect, ALLOWED_SORT_COLUMNS.keySet());
    }

    public String map(AmountRequest request) {
        if (request instanceof OffsetBasedPageRequest) {
            return sqlOffsetBasedPageRequestMapper.mapToSqlQuery((OffsetBasedPageRequest) request, jobTable);
        } else {
            return sqlAmountRequestMapper.mapToSqlQuery(request, jobTable);
        }
    }

}
