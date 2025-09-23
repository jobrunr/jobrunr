package org.jobrunr.storage;

import org.jobrunr.storage.navigation.PageRequest;

import java.util.ArrayList;
import java.util.List;

import static org.jobrunr.utils.CollectionUtils.isNotNullOrEmpty;

public class Page<T> {

    private final Long total;
    private final int currentPage;
    private final int totalPages;
    private final int limit;
    private final long offset;
    private final boolean hasPrevious;
    private final boolean hasNext;
    private final String previousPageRequest;
    private final String nextPageRequest;
    private final ArrayList<T> items;

    public Page(long total, List<T> items, PageRequest currentPage, PageRequest previousPageRequest, PageRequest nextPageRequest) {
        this(total, items, -1, -1, currentPage, previousPageRequest, nextPageRequest);
    }

    public Page(long total, List<T> items, long offset, int currentPageNo, PageRequest currentPage, PageRequest previousPageRequest, PageRequest nextPageRequest) {
        this.total = total;
        this.totalPages = calculateTotalPages(total, currentPage);
        this.limit = calculateLimit(currentPage);
        this.offset = offset;
        this.currentPage = currentPageNo;
        this.hasPrevious = previousPageRequest != null;
        this.hasNext = nextPageRequest != null;
        this.previousPageRequest = hasPrevious ? previousPageRequest.asString() : null;
        this.nextPageRequest = hasNext ? nextPageRequest.asString() : null;
        this.items = new ArrayList<>(items);
    }

    public Long getTotal() {
        return total;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getLimit() {
        return limit;
    }

    public long getOffset() {
        return offset;
    }

    public String getPreviousPageRequest() {
        return previousPageRequest;
    }

    public String getNextPageRequest() {
        return nextPageRequest;
    }

    public List<T> getItems() {
        return items;
    }

    public boolean hasItems() {
        return isNotNullOrEmpty(items);
    }

    public boolean hasPreviousPage() {
        return hasPrevious;
    }

    public boolean hasNextPage() {
        return hasNext;
    }

    private static int calculateTotalPages(long total, PageRequest currentPageRequest) {
        if (total == 0L) return 0;
        int result = (int) Math.ceil((double) total / currentPageRequest.getLimit());
        if (result * ((long) currentPageRequest.getLimit()) < total) result++;
        return result;
    }

    private static int calculateLimit(PageRequest currentPageRequest) {
        if (currentPageRequest == null) return 0;
        return currentPageRequest.getLimit();
    }
}