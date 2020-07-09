package org.jobrunr.storage;

public class PageRequest {
    private static final String DEFAULT_ORDER_FIELD = "createdAt";

    public enum Order {
        ASC,
        DESC
    }

    private long offset = 0;
    private int limit = 20;
    private String orderOnField = DEFAULT_ORDER_FIELD;
    private Order order = Order.ASC;

    public static PageRequest ascOnCreatedAt(int amount) {
        return ascOnCreatedAt(0, amount);
    }

    public static PageRequest ascOnCreatedAt(long offset, int limit) {
        return new PageRequest(DEFAULT_ORDER_FIELD, Order.ASC, offset, limit);
    }

    public static PageRequest descOnCreatedAt(long offset, int limit) {
        return new PageRequest(DEFAULT_ORDER_FIELD, Order.DESC, offset, limit);
    }

    private PageRequest() {
    }

    private PageRequest(String orderOnField, Order order, long offset, int limit) {
        this.orderOnField = orderOnField;
        this.order = order;
        this.offset = offset;
        this.limit = limit;
    }

    public String getOrderField() {
        return orderOnField;
    }

    public Order getOrder() {
        return order;
    }

    public long getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    @Override
    public String toString() {
        return "PageRequest{" +
                "order=" + order +
                ", offset=" + offset +
                ", limit=" + limit +
                '}';
    }
}
