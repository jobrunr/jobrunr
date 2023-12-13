package org.jobrunr.storage;

import org.jobrunr.storage.navigation.PageRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jobrunr.utils.CollectionUtils.isNotNullOrEmpty;

public class Page<T> {

    public static Page emptyPage() {
        return new Page(0, Collections.emptyList(), null, null, null);
    }

    private final Long total;
    private final int currentPage;
    private final int totalPages;
    private final int limit;
    private final long offset;
    private final boolean hasPrevious;
    private final boolean hasNext;
    private final String previousPage;
    private final String nextPage;
    private final ArrayList<T> items;

    public Page(long total, List<T> items, PageRequest currentPage, PageRequest previousPage, PageRequest nextPage) {
        this(total, items, -1, -1, currentPage, previousPage, nextPage);
    }

    public Page(long total, List<T> items, long offset, int currentPageNo, PageRequest currentPage, PageRequest previousPage, PageRequest nextPage) {
        this.total = total;
        this.totalPages = calculateTotalPages(total, currentPage);
        this.limit = calculateLimit(currentPage);
        this.offset = offset;
        this.currentPage = currentPageNo;
        this.hasPrevious = previousPage != null;
        this.hasNext = nextPage != null;
        this.previousPage = (hasPrevious) ? previousPage.asString() : null;
        this.nextPage = (hasNext) ? nextPage.asString() : null;
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

    public String getPreviousPage() {
        return previousPage;
    }

    public String getNextPage() {
        return nextPage;
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
        if (result * currentPageRequest.getLimit() < total) result++;
        return result;
    }

    private static int calculateLimit(PageRequest currentPageRequest) {
        if (currentPageRequest == null) return 0;
        return currentPageRequest.getLimit();
    }
}