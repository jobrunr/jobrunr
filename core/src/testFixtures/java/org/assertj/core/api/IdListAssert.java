package org.assertj.core.api;

import java.util.List;
import java.util.function.Predicate;

public class IdListAssert<T> extends ListAssert<T> {

    public IdListAssert(List<? extends T> actual) {
        super(actual);
    }

    @Override
    public IdListAssert<T> hasSize(int expected) {
        return (IdListAssert<T>) super.hasSize(expected);
    }

    @Override
    public IdListAssert<T> allMatch(Predicate<? super T> predicate) {
        return (IdListAssert<T>) super.allMatch(predicate);
    }

    public IdListAssert containsExactlyComparingById(T... t) {
        usingElementComparatorOnFields("id")
                .containsExactly(t);
        return this;
    }
}
