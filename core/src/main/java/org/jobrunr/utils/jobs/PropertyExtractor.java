package org.jobrunr.utils.jobs;

import org.jobrunr.storage.navigation.OrderTerm.Order;

import java.util.Comparator;
import java.util.function.Function;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;

public class PropertyExtractor<T, U extends Comparable<? super U>> {

    private final Function<T, U> propertyExtractor;

    public PropertyExtractor(Function<T, U> propertyExtractor) {
        this.propertyExtractor = propertyExtractor;
    }

    public Comparator<T> asComparator(Order order) {
        if (order == Order.ASC) return Comparator.comparing(propertyExtractor, nullsLast(naturalOrder()));
        return Comparator.comparing(propertyExtractor).reversed();
    }
}
