package org.jobrunr.storage.nosql.mongo.mapper;

import com.mongodb.client.model.Sorts;
import org.bson.conversions.Bson;
import org.jobrunr.storage.navigation.AmountRequest;
import org.jobrunr.storage.navigation.OrderTerm;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;
import static org.jobrunr.jobs.Job.ALLOWED_SORT_COLUMNS;

public class MongoDBAmountRequestMapper {

    public Bson mapToSort(AmountRequest amountRequest) {
        List<OrderTerm> orderTerms = amountRequest.getAllOrderTerms(ALLOWED_SORT_COLUMNS.keySet());
        List<Bson> result = new ArrayList<>();
        for (OrderTerm orderTerm : orderTerms) {
            result.add(OrderTerm.Order.ASC == orderTerm.getOrder() ? ascending(orderTerm.getFieldName()) : descending(orderTerm.getFieldName()));
        }
        if (result.size() == 1) {
            return result.get(0);
        }
        return Sorts.orderBy(result);
    }

}
