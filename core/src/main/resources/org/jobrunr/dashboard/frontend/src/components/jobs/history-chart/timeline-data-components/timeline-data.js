import {comparePreciseDates, dateAsMilliseconds} from "../../../../utils/helper-functions.js";
import {AWAITING, DELETED, END_STATES, RUN_STEP_ONCE, SCHEDULED} from "../../../utils/state-names.js";
import {TIMELINE_COMPRESSION_MODES, TIMELINE_MODES} from "../job-history-chart.js";
import {addSkippedStepsToPerformedSteps, detectSkippedSteps} from "./determine-skipped-steps.js";
import {createBarPlacements, createCompressedAxis, createTimeCompressor, detectLongRangesToCompress, generateTimeTicks} from "./timeline-axis.js";
import {buildCompactRetryEvents, buildCompactRows, buildDetailedRows} from "./timeline-rows.js";

export {getStepLabel, groupCompactStepsSequentially} from "./timeline-rows.js";
export {createTimeCompressor, generateTimeTicks};

export const EXCLUDED_STATES = [AWAITING, DELETED];

const MIN_COMPRESSION_THRESHOLD_MS = 60000;
const COMPRESSION_THRESHOLD = 0.15;

export const getStepEndTime = (step) => step.updatedAt && comparePreciseDates(step.updatedAt, step.createdAt) > 0 ? dateAsMilliseconds(step.updatedAt) : null;

export const removeInitialScheduled = (steps) => {
    const list = steps ?? [];
    return list.length > 0 && list[0].state === SCHEDULED ? list.slice(1) : list;
};

const recordRunStepOnceAttempt = (historyForRunStepOnce, stepOrder, step, stepStart) => {
    const [stepBase, attemptId] = step.stepName.split('__');
    if (!historyForRunStepOnce.has(stepBase)) {
        historyForRunStepOnce.set(stepBase, []);
        stepOrder.push(stepBase);
    }
    historyForRunStepOnce.get(stepBase).push({attemptId: +attemptId, succeeded: step.succeeded !== false, startMs: stepStart, startAt: step.createdAt});
};

function determineEndTimeBasedOnStepType(step, stepStart, isJobInProgress, now, nextStep) {
    let stepEnd;
    let isStepActive = false;
    if (END_STATES.includes(step.state)) {
        stepEnd = stepStart;
    } else if (step.state === RUN_STEP_ONCE) {
        const endTime = getStepEndTime(step);
        stepEnd = endTime ?? (isJobInProgress ? now : null);
        isStepActive = endTime === null && isJobInProgress;
    } else {
        stepEnd = nextStep ? dateAsMilliseconds(nextStep.createdAt) : (isJobInProgress ? now : getStepEndTime(step));
        isStepActive = !nextStep && isJobInProgress;
    }
    return {stepEnd, active: isStepActive};
}

const computeStepBounds = (steps, now) => {
    const isJobInProgress = steps.length > 0 && !END_STATES.includes(steps[steps.length - 1].state);
    let start = Infinity;
    let end = -Infinity;
    const stepEndTimesMap = new Map();
    const historyForRunStepOnce = new Map();
    const stepOrder = [];

    steps.forEach((step, i) => {
        const stepStart = dateAsMilliseconds(step.createdAt);
        if (stepStart < start) start = stepStart;
        const nextStep = steps.slice(i + 1).find((s) => s.state !== RUN_STEP_ONCE);

        if (step.state === RUN_STEP_ONCE) recordRunStepOnceAttempt(historyForRunStepOnce, stepOrder, step, stepStart);
        let {stepEnd, active} = determineEndTimeBasedOnStepType(step, stepStart, isJobInProgress, now, nextStep);
        if (stepEnd !== null && stepEnd > end) end = stepEnd;
        stepEndTimesMap.set(step, {end: stepEnd, active});
    });

    return {start, end, stepEndTimesMap, historyForRunStepOnce, stepOrder};
};

export const convertStepsToTimeline = (steps, now) => {
    const {start, end, stepEndTimesMap, historyForRunStepOnce, stepOrder} = computeStepBounds(steps, now);
    return {start: Math.min(start, end), end: Math.max(start, end), stepEndTimesMap, skipped: detectSkippedSteps(historyForRunStepOnce, stepOrder)};
};

export const filterStepsForTimeline = (executionSteps) =>
    executionSteps.filter((step) => !EXCLUDED_STATES.includes(step.state));

export const buildTimelineModel = ({steps, mode, compression, reverse, now}) => {
    const filteredSteps = (steps ?? []).filter((step) => !EXCLUDED_STATES.includes(step.state));
    if (filteredSteps.length === 0) return null;

    const {start, end, stepEndTimesMap, skipped} = convertStepsToTimeline(filteredSteps, now);
    const detailedSteps = skipped.length && mode !== TIMELINE_MODES.COMPACT ? addSkippedStepsToPerformedSteps(filteredSteps, skipped) : filteredSteps;
    const duration = end - start;
    const compressionThresholdMs = Math.max(MIN_COMPRESSION_THRESHOLD_MS, duration * COMPRESSION_THRESHOLD);

    const longRanges = detectLongRangesToCompress(filteredSteps, stepEndTimesMap, start, end, now, compressionThresholdMs);
    const compressRanges = compression === TIMELINE_COMPRESSION_MODES.LINEAR ? [] : longRanges;
    const compressTime = createTimeCompressor(compressRanges, duration, compressionThresholdMs);
    const compressedTimelineStart = compressTime(start);
    const compressedTimelineDuration = compressTime(end) - compressedTimelineStart;
    const axis = createCompressedAxis(compressTime, compressedTimelineStart, compressedTimelineDuration);
    const getPlacement = createBarPlacements({axis, compressRanges, reverse});

    return {
        start, end, duration,
        ticks: generateTimeTicks(duration, compressTime, start, compressedTimelineDuration, compressRanges),
        retryEvents: buildCompactRetryEvents(filteredSteps, axis),
        compactRows: mode === TIMELINE_MODES.COMPACT ? buildCompactRows(filteredSteps, stepEndTimesMap, now, skipped, getPlacement, reverse) : [],
        orderedDetailedRows: buildDetailedRows(detailedSteps, stepEndTimesMap, getPlacement, reverse),
    };
};
