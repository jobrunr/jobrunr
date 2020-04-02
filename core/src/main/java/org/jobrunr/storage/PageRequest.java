package org.jobrunr.storage;

public class PageRequest {

    private long offset = 0;
    private int limit = 20;

    public static PageRequest of(long offset, int limit) {
        return new PageRequest(offset, limit);
    }

    private PageRequest() {
    }

    private PageRequest(long offset, int limit) {
        this.offset = offset;
        this.limit = limit;
    }

    public long getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }
}
