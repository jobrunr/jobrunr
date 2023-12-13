package org.jobrunr.storage.sql.common.mapper;

import org.jobrunr.storage.navigation.OffsetBasedPageRequest;
import org.jobrunr.storage.sql.common.db.Dialect;
import org.jobrunr.storage.sql.common.db.Sql;

import java.util.Set;

public class SqlOffsetBasedPageRequestMapper extends SqlAmountRequestMapper {

    public SqlOffsetBasedPageRequestMapper(Dialect dialect, Set<String> allowedSortColumns) {
        super(dialect, allowedSortColumns);
    }

    public String mapToSqlQuery(OffsetBasedPageRequest pageRequest, Sql table) {
        table.with("limit", pageRequest.getLimit());
        table.with("offset", pageRequest.getOffset());
        return orderClause(pageRequest) + " " + dialect.limitAndOffset();
    }

}