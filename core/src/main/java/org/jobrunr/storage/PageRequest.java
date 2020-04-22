package org.jobrunr.storage;

public class PageRequest {

    public enum Order {
        ASC,
        DESC
    }

    private long offset = 0;
    private int limit = 20;
    private Order order = Order.ASC;

    public static PageRequest asc(long offset, int limit) {
        return new PageRequest(offset, limit, Order.ASC);
    }

    public static PageRequest desc(long offset, int limit) {
        return new PageRequest(offset, limit, Order.DESC);
    }

    private PageRequest() {
    }

    private PageRequest(long offset, int limit, Order order) {
        this.offset = offset;
        this.limit = limit;
        this.order = order;
    }

    public long getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    public Order getOrder() {
        return order;
    }
}
