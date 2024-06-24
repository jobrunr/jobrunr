package org.jobrunr.scheduling;

import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.scheduling.Schedule.CarbonAwareScheduleMargin;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.scheduling.Schedule.CarbonAwareScheduleMargin.after;
import static org.jobrunr.scheduling.Schedule.CarbonAwareScheduleMargin.before;
import static org.jobrunr.scheduling.Schedule.CarbonAwareScheduleMargin.margin;
import static org.jobrunr.scheduling.Schedule.CarbonAwareScheduleMargin.parse;

class CarbonAwareScheduleMarginTest {

    @Test
    void parseScheduleExpression() {
        assertThat(parse("")).isNull();
        assertThat(parse(null)).isNull();
        assertThat(parse("PT10H")).isNull();
        assertThat(parse("*/5 * * * *")).isNull();
        assertThat(parse("*/5 * * * * [")).isNull();
        assertThat(parse("*/5 * * * * ]")).isNull();
        assertThat(parse("*/5 * * * * [PT10H")).isNull();
        assertThat(parse("*/5 * * * * PT10H]")).isNull();
        assertThat(parse("*/5 * * * * [PT10H/")).isNull();
        assertThat(parse("*/5 * * * * /PT10H]")).isNull();

        assertThatCode(() -> parse("[PT2H]"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Found String (='PT2H') formatted as a CarbonAwareScheduleMargin which does not have the expected length of 2 (got length=1).");
        assertThatCode(() -> parse("[PT2H/]"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Found String (='PT2H/') formatted as a CarbonAwareScheduleMargin which does not have the expected length of 2 (got length=1).");
        assertThatCode(() -> parse("[PT2H/PT2H/PT2S]"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Found String (='PT2H/PT2H/PT2S') formatted as a CarbonAwareScheduleMargin which does not have the expected length of 2 (got length=3).");

        assertThatCode(() -> parse("[0 0 1 * */PT20H]"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cannot parse marginBefore (='0 0 1 * *') and marginAfter (='PT20H') of the CarbonAwareScheduleMargin string (='0 0 1 * */PT20H') as Durations.");

        assertThatCode(() -> parse("[-PT2H/PT2H]"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected marginBefore (='PT-2H') and marginAfter (='PT2H') to be positive Durations.");
        assertThatCode(() -> parse("[PT2H/PT0S]"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Expected marginBefore (='PT2H') and marginAfter (='PT0S') to span at least 3 hours so a moment of low carbon emissions can be optimally selected.");

        assertThat(parse("[PT10H/PT0S]"))
                .isNotNull()
                .usingRecursiveComparison()
                .isEqualTo(margin(Duration.ofHours(10), Duration.ZERO));
        assertThat(parse("0 0 1 * * [PT2H/PT1H]"))
                .isNotNull()
                .usingRecursiveComparison()
                .isEqualTo(margin(Duration.ofHours(2), Duration.ofHours(1)));
        assertThat(parse("0 0 1 * * [ PT3H / PT0S ] "))
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
    void toCarbonAwareAwaitingState() {
        CarbonAwareScheduleMargin carbonAwareScheduleMargin = margin(Duration.ofHours(2), Duration.ofHours(10));

        Instant now = Instant.now();
        assertThat(carbonAwareScheduleMargin.toCarbonAwareAwaitingState(now))
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(new CarbonAwareAwaitingState(now, now.minus(Duration.ofHours(2)), now.plus(Duration.ofHours(10))));

        Instant twoDaysLater = Instant.now().plus(2, ChronoUnit.DAYS);
        assertThat(carbonAwareScheduleMargin.toCarbonAwareAwaitingState(twoDaysLater))
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(new CarbonAwareAwaitingState(twoDaysLater, twoDaysLater.minus(Duration.ofHours(2)), twoDaysLater.plus(Duration.ofHours(10))));
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