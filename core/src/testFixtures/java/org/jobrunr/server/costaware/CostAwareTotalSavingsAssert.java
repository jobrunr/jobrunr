package org.jobrunr.server.costaware;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class CostAwareTotalSavingsAssert extends AbstractAssert<CostAwareTotalSavingsAssert, CostAwareTotalSavings> {

    protected CostAwareTotalSavingsAssert(CostAwareTotalSavings costAwareTotalSavings) {
        super(costAwareTotalSavings, CostAwareTotalSavingsAssert.class);
    }

    public static CostAwareTotalSavingsAssert assertThat(CostAwareTotalSavings costAwareTotalSavings) {
        return new CostAwareTotalSavingsAssert(costAwareTotalSavings);
    }

    public CostAwareTotalSavingsAssert isNotNull() {
        Assertions.assertThat(actual).isNotNull();
        return this;
    }

    public CostAwareTotalSavingsAssert hasAmountOfDailySavings(int amount) {
        Assertions.assertThat(actual.getDailySavings().size()).isEqualTo(amount);
        return this;
    }

    public CostAwareTotalSavingsAssert hasAmountOfMonthlySavings(int amount) {
        Assertions.assertThat(actual.getMonthlySavings().size()).isEqualTo(amount);
        return this;
    }

    public CostAwareTotalSavingsAssert hasAmountOfYearlySavings(int amount) {
        Assertions.assertThat(actual.getYearlySavings().size()).isEqualTo(amount);
        return this;
    }
}
