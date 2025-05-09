package io.micrometer.core.instrument;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class MeterAssert extends AbstractAssert<MeterAssert, Meter> {
    protected MeterAssert(Meter meter) {
        super(meter, MeterAssert.class);
    }

    public static MeterAssert assertThat(Meter meter) {
        return new MeterAssert(meter);
    }

    public MeterAssert hasIdWithTag(String tagName, String tagValue) {
        Assertions.assertThat(actual.getId().getTag(tagName))
                .describedAs("Could not find tag " + tagName + " on meter with id " + actual.getId())
                .isNotNull()
                .isEqualTo(tagValue);
        return this;
    }
}
