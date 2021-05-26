package org.jobrunr.storage.nosql.mongo.mapper;

import com.mongodb.client.model.Sorts;
import org.bson.conversions.Bson;
import org.jobrunr.storage.PageRequest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_CREATED_AT;
import static org.jobrunr.storage.StorageProviderUtils.Jobs.FIELD_UPDATED_AT;

public class MongoDBPageRequestMapper {

    private static final Set<String> allowedSortColumns = new HashSet<>();

    static {
        allowedSortColumns.add(FIELD_CREATED_AT);
        allowedSortColumns.add(FIELD_UPDATED_AT);
    }

    public Bson map(PageRequest pageRequest) {
        final List<Bson> result = new ArrayList<>();
        final String[] sortOns = pageRequest.getOrder().split(",");
        for (String sortOn : sortOns) {
            final String[] sortAndOrder = sortOn.split(":");
            if (!allowedSortColumns.contains(sortAndOrder[0])) continue;
            String sortField = sortAndOrder[0];
            PageRequest.Order order = PageRequest.Order.ASC;
            if (sortAndOrder.length > 1) {
                order = PageRequest.Order.valueOf(sortAndOrder[1].toUpperCase());
            }
            result.add(order == PageRequest.Order.ASC ? ascending(sortField) : descending(sortField));
        }
        return Sorts.orderBy(result);
    }

}
