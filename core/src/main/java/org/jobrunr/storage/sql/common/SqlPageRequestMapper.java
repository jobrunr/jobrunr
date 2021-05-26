package org.jobrunr.storage.sql.common;

import org.jobrunr.storage.PageRequest;

import java.util.HashSet;
import java.util.Set;

import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_CREATED_AT;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_UPDATED_AT;

public class SqlPageRequestMapper {

    private static final Set<String> allowedSortColumns = new HashSet<>();

    static {
        allowedSortColumns.add(FIELD_CREATED_AT);
        allowedSortColumns.add(FIELD_UPDATED_AT);
    }

    public String map(PageRequest pageRequest) {
        final StringBuilder result = new StringBuilder();
        final String[] sortOns = pageRequest.getOrder().split(",");
        for (String sortOn : sortOns) {
            final String[] sortAndOrder = sortOn.split(":");
            if (!allowedSortColumns.contains(sortAndOrder[0])) continue;
            String sortField = sortAndOrder[0];
            PageRequest.Order order = PageRequest.Order.ASC;
            if (sortAndOrder.length > 1) {
                order = PageRequest.Order.valueOf(sortAndOrder[1].toUpperCase());
            }
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(sortField).append(" ").append(order.name());
        }
        return result.toString();
    }

}
