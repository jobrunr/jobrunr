package org.jobrunr.storage.navigation;

import org.jobrunr.storage.Page;

import java.util.List;

public abstract class PageRequest extends AmountRequest {

    protected PageRequest(String order, Integer limit) {
        super(order, limit);
    }

    public abstract <T> Page<T> mapToNewPage(long count, List<T> items);

    public abstract <T> Page<T> emptyPage();
}