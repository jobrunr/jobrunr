package org.jobrunr.utils.jobs;

import org.jobrunr.storage.navigation.OrderTerm;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public abstract class AbstractSortColumns<T> {

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

    public Comparator<T> toComparator(OrderTerm orderTerm) {
        PropertyExtractor<T, ?> propertyExtractor = allowedSortColumns.get(orderTerm.getFieldName());
        return propertyExtractor.asComparator(orderTerm.getOrder());
    }
}
