package org.jobrunr.storage.navigation;

import org.jobrunr.storage.Page;

import java.util.List;

public abstract class PageRequest extends AmountRequest {

    public PageRequest(String order, int limit) {
        super(order, limit);
    }

    public abstract <T> Page<T> mapToNewPage(long count, List<T> items);

    public abstract <T> Page<T> emptyPage();
}