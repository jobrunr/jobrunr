package org.jobrunr.scheduling.carbonaware;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.time.temporal.Temporal;

public class CarbonAwarePeriodAssert extends AbstractAssert<CarbonAwarePeriodAssert, CarbonAwarePeriod> {

    protected CarbonAwarePeriodAssert(CarbonAwarePeriod carbonAwarePeriod) {
        super(carbonAwarePeriod, CarbonAwarePeriodAssert.class);
    }

    public static CarbonAwarePeriodAssert assertThat(CarbonAwarePeriod carbonAwarePeriod) {
        return new CarbonAwarePeriodAssert(carbonAwarePeriod);
    }

    public CarbonAwarePeriodAssert hasFrom(Temporal from) {
        Assertions.assertThat(actual.getFrom()).isEqualTo(from);
        return this;
    }

    public CarbonAwarePeriodAssert hasTo(Temporal to) {
        Assertions.assertThat(actual.getTo()).isEqualTo(to);
        return this;
    }
}
