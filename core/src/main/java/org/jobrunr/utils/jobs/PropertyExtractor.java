package org.jobrunr.utils.jobs;

import org.jobrunr.jobs.AbstractJob;

import java.util.Comparator;
import java.util.function.Function;

public class PropertyExtractor<T extends AbstractJob, U extends Comparable<? super U>> {

    private final Function<T, U> propertyExtractor;

    public PropertyExtractor(Function<T, U> propertyExtractor) {
        this.propertyExtractor = propertyExtractor;
    }

    public U extract(T job) {
        return propertyExtractor.apply(job);
    }

    public Comparator<T> asComparator() {
        return Comparator.comparing(propertyExtractor);
    }
}
