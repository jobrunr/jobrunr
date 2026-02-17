package org.jobrunr.storage.navigation;

import org.jobrunr.storage.Page;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.jobrunr.utils.StringUtils.isNotNullOrEmpty;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;
import static org.jobrunr.utils.StringUtils.lenientSubstringBetween;

public class OffsetBasedPageRequest extends PageRequest {

    public static final long DEFAULT_OFFSET = 0L;

    private final long offset;

    OffsetBasedPageRequest() {
        this(null, DEFAULT_OFFSET, DEFAULT_LIMIT); // needed for deserialization
    }

    public OffsetBasedPageRequest(String order, long offset, int limit) {
        super(order, limit);
        this.offset = offset;
    }

    public long getOffset() {
        return offset;
    }

    @Override
    public String asString() {
        return "order=" + getOrder() + "&offset=" + offset + "&limit=" + getLimit();
    }

    public static OffsetBasedPageRequest fromString(String offsetBasedPageRequestAsString) {
        if (isNullOrEmpty(offsetBasedPageRequestAsString)) return null;

        String order = lenientSubstringBetween(offsetBasedPageRequestAsString, "order=", "&");
        String offset = lenientSubstringBetween(offsetBasedPageRequestAsString, "offset=", "&");
        String limit = lenientSubstringBetween(offsetBasedPageRequestAsString, "limit=", "&");
        return new OffsetBasedPageRequest(
                order,
                isNotNullOrEmpty(offset) ? Integer.parseInt(offset) : 0L,
                isNotNullOrEmpty(limit) ? Integer.parseInt(limit) : DEFAULT_LIMIT
        );
    }

    @Override
    public <T> Page<T> mapToNewPage(long total, List<T> items) {
        return new Page<>(total, items, offset, calculateCurrentPage(), this, previousPageRequest(total), nextPageRequest(total));
    }

    @Override
    public <T> Page<T> emptyPage() {
        return new Page<>(0, emptyList(), offset, 0, this, null, null);
    }

    private OffsetBasedPageRequest previousPageRequest(long ignored) {
        if (getOffset() >= getLimit()) {
            return new OffsetBasedPageRequest(getOrder(), getOffset() - getLimit(), getLimit());
        }
        return null;
    }

    private OffsetBasedPageRequest nextPageRequest(long total) {
        if (total > (getOffset() + getLimit())) {
            return new OffsetBasedPageRequest(getOrder(), getOffset() + getLimit(), getLimit());
        }
        return null;
    }

    private int calculateCurrentPage() {
        int result = (int) Math.floor((double) offset / limit);
        if (result < 1 && offset > 0) {
            return 1;
        }
        return result;
    }
}