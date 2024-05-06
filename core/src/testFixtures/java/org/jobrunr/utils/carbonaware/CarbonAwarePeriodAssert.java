package org.jobrunr.utils.carbonaware;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.time.Instant;

public class CarbonAwarePeriodAssert extends AbstractAssert<CarbonAwarePeriodAssert, CarbonAwarePeriod> {

    protected CarbonAwarePeriodAssert(CarbonAwarePeriod carbonAwarePeriod) {
        super(carbonAwarePeriod, CarbonAwarePeriodAssert.class);
    }

    public static CarbonAwarePeriodAssert assertThat(CarbonAwarePeriod carbonAwarePeriod) {
        return new CarbonAwarePeriodAssert(carbonAwarePeriod);
    }

    public CarbonAwarePeriodAssert hasFrom(Instant from) {
        Assertions.assertThat(actual.getFrom()).isEqualTo(from);
        return this;
    }

    public CarbonAwarePeriodAssert hasTo(Instant to) {
        Assertions.assertThat(actual.getTo()).isEqualTo(to);
        return this;
    }
}
