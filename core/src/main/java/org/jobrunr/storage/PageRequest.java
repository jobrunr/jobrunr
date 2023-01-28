package org.jobrunr.storage;

public class PageRequest {
    private static final String DEFAULT_ORDER_FIELD = "updatedAt";

    public enum Order {
        ASC,
        DESC
    }

    private long offset = 0;
    private int limit = 20;
    private String order = DEFAULT_ORDER_FIELD + ":" + Order.ASC.name();

    public static PageRequest ascOnUpdatedAt(int amount) {
        return ascOnUpdatedAt(0, amount);
    }

    public static PageRequest ascOnUpdatedAt(long offset, int limit) {
        return new PageRequest(DEFAULT_ORDER_FIELD + ":" + Order.ASC, offset, limit);
    }

    public static PageRequest descOnUpdatedAt(int amount) {
        return descOnUpdatedAt(0, amount);
    }

    public static PageRequest descOnUpdatedAt(long offset, int limit) {
        return new PageRequest(DEFAULT_ORDER_FIELD + ":" + Order.DESC, offset, limit);
    }

    private PageRequest() {
    }

    public PageRequest(String order, long offset, int limit) {
        this.order = order;
        this.offset = offset;
        this.limit = limit;
    }

    public String getOrder() {
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

    public PageRequest nextPage() {
        return new PageRequest(order, offset + limit, limit);
    }
}
