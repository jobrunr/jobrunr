package org.jobrunr.storage.navigation;

import org.jobrunr.utils.annotations.VisibleFor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;
import static org.jobrunr.utils.StringUtils.lenientSubstringBetween;
import static org.jobrunr.utils.StringUtils.substringAfter;
import static org.jobrunr.utils.StringUtils.substringBefore;

public class AmountRequest {

    public static final int DEFAULT_LIMIT = 20;

    protected final String order;
    protected final int limit;

    public static AmountRequest fromString(String amountRequestAsString) {
        if (isNullOrEmpty(amountRequestAsString)) return null;

        return new AmountRequest(
                lenientSubstringBetween(amountRequestAsString, "order=", "&"),
                Integer.parseInt(lenientSubstringBetween(amountRequestAsString, "limit=", "&"))
        );
    }

    public AmountRequest(String order, int limit) {
        this.order = isNotNullOrEmpty(order) ? order : "updatedAt:ASC";
        this.limit = limit;
    }

    public String getOrder() {
        return order;
    }

    public int getLimit() {
        return limit;
    }

    public String asString() {
        return "order=" + getOrder() + "&limit=" + getLimit();
    }

    @VisibleFor("testing")
    public List<OrderTerm> getAllOrderTerms(Set<String> allowedOrderTerms) {
        List<OrderTerm> result = new ArrayList<>();
        final String[] sortOns = order.split(",");
        for (String sortOn : sortOns) {
            String sortField = sortField(sortOn);
            if (!allowedOrderTerms.contains(sortField)) continue;
            OrderTerm.Order sortOrder = sortOrder(sortOn);
            result.add(new OrderTerm(sortField, sortOrder));
        }
        return result;
    }

    private String sortField(String sortOn) {
        return substringBefore(sortOn, ":");
    }

    private OrderTerm.Order sortOrder(String sortOn) {
        final String sortOrder = substringAfter(sortOn, ":");
        if (sortOrder == null) return OrderTerm.Order.ASC;
        return OrderTerm.Order.valueOf(sortOrder.toUpperCase());
    }
}