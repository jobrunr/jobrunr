package org.jobrunr.storage.navigation;

public class OrderTerm {
    public enum Order {
        ASC,
        DESC
    }

    private final String fieldName;
    private final Order order;

    public OrderTerm(String fieldName, Order order) {
        this.fieldName = fieldName;
        this.order = order;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Order getOrder() {
        return order;
    }
}