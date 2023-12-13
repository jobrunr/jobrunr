package org.jobrunr.storage.sql.common.mapper;

import org.jobrunr.storage.navigation.AmountRequest;
import org.jobrunr.storage.navigation.OrderTerm;
import org.jobrunr.storage.sql.common.db.Dialect;
import org.jobrunr.storage.sql.common.db.Sql;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;

public class SqlAmountRequestMapper {
    private static final Function<OrderTerm, String> ORDER_TERM_TO_SQL_STRING = orderTerm -> orderTerm.getFieldName() + " " + orderTerm.getOrder();

    protected final Dialect dialect;
    protected final Set<String> allowedSortColumns;

    public SqlAmountRequestMapper(Dialect dialect, Set<String> allowedSortColumns) {
        this.dialect = dialect;
        this.allowedSortColumns = allowedSortColumns;
    }

    public String mapToSqlQuery(AmountRequest pageRequest, Sql table) {
        return mapToSqlQuery(pageRequest, table, ORDER_TERM_TO_SQL_STRING);
    }

    public String mapToSqlQuery(AmountRequest amountRequest, Sql table, Function<OrderTerm, String> orderTermMapper) {
        table.with("limit", amountRequest.getLimit());
        return orderClause(amountRequest, orderTermMapper) + " " + dialect.limit();
    }

    public String orderClause(AmountRequest amountRequest) {
        return orderClause(amountRequest, ORDER_TERM_TO_SQL_STRING);
    }

    private String orderClause(AmountRequest amountRequest, Function<OrderTerm, String> orderTermMapper) {
        List<OrderTerm> orderTerms = amountRequest.getAllOrderTerms(allowedSortColumns);
        if (orderTerms.isEmpty()) return "";
        return " ORDER BY " + orderTerms.stream()
                .map(orderTermMapper)
                .collect(joining(", "));
    }
}