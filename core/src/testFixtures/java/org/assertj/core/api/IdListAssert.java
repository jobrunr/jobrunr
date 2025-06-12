package org.assertj.core.api;

import org.jobrunr.jobs.AbstractJob;

import java.util.List;

public class IdListAssert<ELEMENT extends AbstractJob, ELEMENT_ASSERT extends AbstractAssert<ELEMENT_ASSERT, ELEMENT>> extends FactoryBasedNavigableListAssert<IdListAssert<ELEMENT, ELEMENT_ASSERT>, List<? extends ELEMENT>, ELEMENT, ELEMENT_ASSERT> {

    public IdListAssert(List<? extends ELEMENT> elements, AssertFactory<ELEMENT, ELEMENT_ASSERT> assertFactory) {
        super(elements, IdListAssert.class, assertFactory);
    }

    @SafeVarargs
    public final IdListAssert<ELEMENT, ELEMENT_ASSERT> containsExactlyComparingById(ELEMENT... t) {
        usingRecursiveFieldByFieldElementComparatorOnFields("id")
                .containsExactly(t);
        return this;
    }
}