package org.jobrunr.utils.jobs;

import org.jobrunr.jobs.AbstractJob;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public abstract class AbstractSortColumns<T extends AbstractJob> {

    protected final Map<String, PropertyExtractor<T, ?>> allowedSortColumns;

    public AbstractSortColumns() {
        this.allowedSortColumns = new HashMap<>();
    }

    public <P extends Comparable<P>> void add(String field, Function<T, P> extractor) {
        this.allowedSortColumns.put(field, new PropertyExtractor<>(extractor));
    }

    public Set<String> keySet() {
        return allowedSortColumns.keySet();
    }

    public PropertyExtractor<T, ?> get(String key) {
        return allowedSortColumns.get(key);
    }
}
