package org.jobrunr.storage;

import java.util.ArrayList;
import java.util.List;

public class Page<T> {

    private final long total;
    private final long offset;
    private final int limit;
    private final ArrayList<T> items;
    private final int currentPage;
    private final int totalPages;
    private final boolean hasPrevious;
    private final boolean hasNext;

    public Page(long total, List<T> items, PageRequest pageRequest) {
        this(total, items, pageRequest.getOffset(), pageRequest.getLimit());
    }

    public Page(long total, List<T> items, long offset, int limit) {
        this.total = total;
        this.items = new ArrayList<>(items);
        this.offset = offset;
        this.limit = limit;
        this.currentPage = calculateCurrentPage();
        this.totalPages = calculateTotalPages();
        this.hasPrevious = offset > 0;
        this.hasNext = offset + limit < total;
    }

    public long getTotal() {
        return total;
    }

    public long getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    public List<T> getItems() {
        return items;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean hasPrevious() {
        return hasPrevious;
    }

    public boolean hasNext() {
        return hasNext;
    }

    private int calculateCurrentPage() {
        int result = (int) Math.floor((double) offset / limit);
        if(result < 1 && offset > 0) {
            return 1;
        }
        return result;
    }

    private int calculateTotalPages() {
        int result = (int) Math.ceil((double) total / limit);
        if (result * limit < total) result++;
        return result;
    }
}
