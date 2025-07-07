package org.jobrunr.scheduling.carbonaware;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.scheduling.carbonaware.CarbonAwareScheduleMargin.after;
import static org.jobrunr.scheduling.carbonaware.CarbonAwareScheduleMargin.before;
import static org.jobrunr.scheduling.carbonaware.CarbonAwareScheduleMargin.getCarbonAwareMarginFromScheduleExpression;
import static org.jobrunr.scheduling.carbonaware.CarbonAwareScheduleMargin.getScheduleExpressionWithoutCarbonAwareMargin;
import static org.jobrunr.scheduling.carbonaware.CarbonAwareScheduleMargin.margin;

class CarbonAwareScheduleMarginTest {

    @Test
    void testGetScheduleExpressionWithoutCarbonAwareMargin() {
        assertThat(getScheduleExpressionWithoutCarbonAwareMargin("")).isNull();
        assertThat(getScheduleExpressionWithoutCarbonAwareMargin(null)).isNull();
        assertThat(getScheduleExpressionWithoutCarbonAwareMargin("*/5 * * * *")).isEqualTo("*/5 * * * *");
        assertThat(getScheduleExpressionWithoutCarbonAwareMargin("*/5 * * * * [")).isEqualTo("*/5 * * * * [");
        assertThat(getScheduleExpressionWithoutCarbonAwareMargin("*/5 * * * * ]")).isEqualTo("*/5 * * * * ]");
        assertThat(getScheduleExpressionWithoutCarbonAwareMargin("*/5 * * * * [PT10H")).isEqualTo("*/5 * * * * [PT10H");
        assertThat(getScheduleExpressionWithoutCarbonAwareMargin("*/5 * * * * PT10H]")).isEqualTo("*/5 * * * * PT10H]");
        assertThat(getScheduleExpressionWithoutCarbonAwareMargin("*/5 * * * * [PT10H/")).isEqualTo("*/5 * * * * [PT10H/");
        assertThat(getScheduleExpressionWithoutCarbonAwareMargin("*/5 * * * * /PT10H]")).isEqualTo("*/5 * * * * /PT10H]");
        assertThat(getScheduleExpressionWithoutCarbonAwareMargin("0 0 1 * * [PT2H/PT1H]")).isEqualTo("0 0 1 * *");
        assertThat(getScheduleExpressionWithoutCarbonAwareMargin("0 0 1 * * [ PT3H / PT0S ] ")).isEqualTo("0 0 1 * *");
    }

    @Test
    void testGetCarbonAwareMarginFromScheduleExpression() {
        assertThat(getCarbonAwareMarginFromScheduleExpression("")).isNull();
        assertThat(getCarbonAwareMarginFromScheduleExpression(null)).isNull();
        assertThat(getCarbonAwareMarginFromScheduleExpression("PT10H")).isNull();
        assertThat(getCarbonAwareMarginFromScheduleExpression("*/5 * * * *")).isNull();
        assertThat(getCarbonAwareMarginFromScheduleExpression("*/5 * * * * [")).isNull();
        assertThat(getCarbonAwareMarginFromScheduleExpression("*/5 * * * * ]")).isNull();
        assertThat(getCarbonAwareMarginFromScheduleExpression("*/5 * * * * [PT10H")).isNull();
        assertThat(getCarbonAwareMarginFromScheduleExpression("*/5 * * * * PT10H]")).isNull();
        assertThat(getCarbonAwareMarginFromScheduleExpression("*/5 * * * * [PT10H/")).isNull();
        assertThat(getCarbonAwareMarginFromScheduleExpression("*/5 * * * * /PT10H]")).isNull();

        assertThatCode(() -> getCarbonAwareMarginFromScheduleExpression("[PT2H]"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Found String (='PT2H') formatted as a CarbonAwareScheduleMargin which does not have the expected length of 2 (got length=1).");
        assertThatCode(() -> getCarbonAwareMarginFromScheduleExpression("[PT2H/]"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Found String (='PT2H/') formatted as a CarbonAwareScheduleMargin which does not have the expected length of 2 (got length=1).");
        assertThatCode(() -> getCarbonAwareMarginFromScheduleExpression("[PT2H/PT2H/PT2S]"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Found String (='PT2H/PT2H/PT2S') formatted as a CarbonAwareScheduleMargin which does not have the expected length of 2 (got length=3).");

        assertThatCode(() -> getCarbonAwareMarginFromScheduleExpression("[0 0 1 * */PT20H]"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot parse marginBefore (='0 0 1 * *') and marginAfter (='PT20H') of the CarbonAwareScheduleMargin string (='0 0 1 * */PT20H') as Durations.");

        assertThatCode(() -> getCarbonAwareMarginFromScheduleExpression("[-PT2H/PT2H]"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected marginBefore (='PT-2H') and marginAfter (='PT2H') to be positive Durations.");

        assertThat(getCarbonAwareMarginFromScheduleExpression("[PT10H/PT0S]"))
                .isNotNull()
                .usingRecursiveComparison()
                .isEqualTo(margin(Duration.ofHours(10), Duration.ZERO));
        assertThat(getCarbonAwareMarginFromScheduleExpression("0 0 1 * * [PT2H/PT1H]"))
                .isNotNull()
                .usingRecursiveComparison()
                .isEqualTo(margin(Duration.ofHours(2), Duration.ofHours(1)));
        assertThat(getCarbonAwareMarginFromScheduleExpression("0 0 1 * * [ PT3H / PT0S ] "))
                .isNotNull()
                .usingRecursiveComparison()
                .isEqualTo(before(Duration.ofHours(3)));
    }

    @Test
    void createCarbonAwareScheduleMarginUsingTheStaticMethods() {
        CarbonAwareScheduleMargin carbonAwareScheduleMargin1 = margin(Duration.ofHours(2), Duration.ofHours(2));

        assertThat(carbonAwareScheduleMargin1.getMarginBefore()).isEqualTo(Duration.ofHours(2));
        assertThat(carbonAwareScheduleMargin1.getMarginAfter()).isEqualTo(Duration.ofHours(2));

        CarbonAwareScheduleMargin carbonAwareScheduleMargin2 = before(Duration.ofHours(3));

        assertThat(carbonAwareScheduleMargin2.getMarginBefore()).isEqualTo(Duration.ofHours(3));
        assertThat(carbonAwareScheduleMargin2.getMarginAfter()).isEqualTo(Duration.ZERO);

        CarbonAwareScheduleMargin carbonAwareScheduleMargin3 = after(Duration.ofHours(4));

        assertThat(carbonAwareScheduleMargin3.getMarginBefore()).isEqualTo(Duration.ZERO);
        assertThat(carbonAwareScheduleMargin3.getMarginAfter()).isEqualTo(Duration.ofHours(4));

    }

    @Test
    void toScheduleExpression() {
        CarbonAwareScheduleMargin carbonAwareScheduleMargin = margin(Duration.ofHours(2), Duration.ofHours(10));

        assertThat(carbonAwareScheduleMargin.toScheduleExpression()).isEqualTo("[PT2H/PT10H]");
    }

    @Test
    void toScheduleExpressionPassingAScheduleExpression() {
        CarbonAwareScheduleMargin carbonAwareScheduleMargin = margin(Duration.ofHours(2), Duration.ofHours(1));

        assertThat(carbonAwareScheduleMargin.toScheduleExpression("PT6H")).isEqualTo("PT6H [PT2H/PT1H]");
        assertThat(carbonAwareScheduleMargin.toScheduleExpression("PT9H")).isEqualTo("PT9H [PT2H/PT1H]");
    }

}