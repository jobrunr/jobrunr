package org.jobrunr.scheduling.carbonaware;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.format.DateTimeParseException;

import static java.lang.String.format;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;
import static org.jobrunr.utils.StringUtils.lastMatchedSubstringBetween;
import static org.jobrunr.utils.StringUtils.substringBeforeLast;

public class CarbonAwareScheduleMargin {
    static final String MARGIN_OPENING_TAG = "[";
    static final String MARGIN_DELIMITER = "/";
    static final String MARGIN_CLOSING_TAG = "]";

    private final Duration marginBefore;
    private final Duration marginAfter;

    public CarbonAwareScheduleMargin(@NonNull Duration marginBefore, @NonNull Duration marginAfter) {
        if (marginBefore.isNegative() || marginAfter.isNegative()) {
            throw new IllegalArgumentException(format("Expected marginBefore (='%s') and marginAfter (='%s') to be positive Durations.", marginBefore, marginAfter));
        }
        this.marginBefore = marginBefore;
        this.marginAfter = marginAfter;
    }

    public static @Nullable String getScheduleExpressionWithoutCarbonAwareMargin(String expressionWithOptionalMargin) {
        if (isNullOrEmpty(expressionWithOptionalMargin)) {
            return null;
        } else if (expressionWithOptionalMargin.contains(MARGIN_OPENING_TAG) && expressionWithOptionalMargin.contains(MARGIN_DELIMITER) && expressionWithOptionalMargin.contains(MARGIN_CLOSING_TAG)) {
            return substringBeforeLast(expressionWithOptionalMargin, MARGIN_OPENING_TAG).trim();
        }
        return expressionWithOptionalMargin;
    }

    public static @Nullable CarbonAwareScheduleMargin getCarbonAwareMarginFromScheduleExpression(String scheduleWithOptionalCarbonAwareScheduleMargin) {
        String margin = getCarbonAwareMarginAsStringFromScheduleExpression(scheduleWithOptionalCarbonAwareScheduleMargin);
        if (isNullOrEmpty(margin)) return null;
        String[] splitMargin = margin.split(MARGIN_DELIMITER);
        if (splitMargin.length != 2) {
            throw new IllegalArgumentException(format("Found String (='%s') formatted as a CarbonAwareScheduleMargin which does not have the expected length of 2 (got length=%s).", margin, splitMargin.length));
        }
        try {
            return new CarbonAwareScheduleMargin(Duration.parse(splitMargin[0].trim()), Duration.parse(splitMargin[1].trim()));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(format("Cannot parse marginBefore (='%s') and marginAfter (='%s') of the CarbonAwareScheduleMargin string (='%s') as Durations.", splitMargin[0], splitMargin[1], margin), e);
        }
    }

    public static CarbonAwareScheduleMargin margin(Duration marginBefore, Duration marginAfter) {
        return new CarbonAwareScheduleMargin(marginBefore, marginAfter);
    }

    public static CarbonAwareScheduleMargin before(Duration marginBefore) {
        return new CarbonAwareScheduleMargin(marginBefore, Duration.ZERO);
    }

    public static CarbonAwareScheduleMargin after(Duration marginAfter) {
        return new CarbonAwareScheduleMargin(Duration.ZERO, marginAfter);
    }

    public Duration getMarginBefore() {
        return marginBefore;
    }

    public Duration getMarginAfter() {
        return marginAfter;
    }

    public String toScheduleExpression(String scheduleExpressionWithoutCarbonAwareMargin) {
        return scheduleExpressionWithoutCarbonAwareMargin + " " + toScheduleExpression();
    }

    public String toScheduleExpression() {
        return MARGIN_OPENING_TAG + marginBefore + MARGIN_DELIMITER + marginAfter + MARGIN_CLOSING_TAG;
    }

    @Override
    public String toString() {
        return toScheduleExpression();
    }

    private static String getCarbonAwareMarginAsStringFromScheduleExpression(String scheduleWithOptionalCarbonAwareScheduleMargin) {
        if (isNullOrEmpty(scheduleWithOptionalCarbonAwareScheduleMargin)) return null;
        return lastMatchedSubstringBetween(scheduleWithOptionalCarbonAwareScheduleMargin, MARGIN_OPENING_TAG, MARGIN_CLOSING_TAG);
    }
}
