import {formatDuration, javaDateAsMicroseconds, javaDateAsMilliseconds} from "../../../utils/helper-functions.js";

export const END_STATES = ['SUCCEEDED', 'FAILED'];
export const EXCLUDED_NON_COMPACT = ['AWAITING', 'DELETED'];

const STEP_LABELS = {
    AWAITING: 'Awaiting', SCHEDULED: 'Scheduled', ENQUEUED: 'Enqueued',
    PROCESSING: 'Processing', SUCCEEDED: 'Succeeded', FAILED: 'Failed',
    RUN_STEP_ONCE: 'Step (runStepOnce)',
};
const MIN_COMPRESSION_THRESHOLD_MS = 60000;
const COMPRESSION_THRESHOLD = 0.15;

const lifecycleRows = () => [
    {key: 'SCHEDULED', label: 'Scheduled', isStep: false, items: []},
    {key: 'ENQUEUED', label: 'Enqueued', isStep: false, items: []},
    {key: 'PROCESSING', label: 'Processing', isStep: false, items: []},
];

export const getStepEndTime = (step) => step.updatedAt && javaDateAsMicroseconds(step.updatedAt) > javaDateAsMicroseconds(step.createdAt) ? javaDateAsMilliseconds(step.updatedAt) : null;

export const removeInitialScheduled = (steps) => {
    const list = steps ?? [];
    return list.length > 0 && list[0].state === 'SCHEDULED' ? list.slice(1) : list;
};

export const getStepLabel = (step) => {
    if (step.isConsolidated) return 'Execution time';
    if (step.state === 'RUN_STEP_ONCE' && step.stepName) return step.stepName.split('__')[0];
    return STEP_LABELS[step.state] ?? step.state ?? 'Unknown';
};

const computeStepBounds = (steps, now) => {
    const isJobInProgress = steps.length > 0 && !END_STATES.includes(steps[steps.length - 1].state);
    let start = Infinity;
    let end = -Infinity;
    const stepEndTimesMap = new Map();
    const historyForRunStepOnce = new Map();
    const stepOrder = [];

    steps.forEach((step, i) => {
        const stepStart = javaDateAsMilliseconds(step.createdAt);
        if (stepStart < start) start = stepStart;
        const nextStep = steps.slice(i + 1).find((s) => s.state !== 'RUN_STEP_ONCE');

        let stepEnd;
        let active = false;
        if (END_STATES.includes(step.state)) {
            stepEnd = stepStart;
        } else if (step.state === 'RUN_STEP_ONCE') {
            const endTime = getStepEndTime(step);
            stepEnd = endTime ?? (isJobInProgress ? now : null);
            active = endTime === null && isJobInProgress;
            const [stepBase, attemptId] = step.stepName.split('__');
            if (!historyForRunStepOnce.has(stepBase)) {
                historyForRunStepOnce.set(stepBase, []);
                stepOrder.push(stepBase);
            }
            historyForRunStepOnce.get(stepBase).push({attemptId: +attemptId, succeeded: step.succeeded !== false, startMs: stepStart, startAt: step.createdAt});
        } else {
            stepEnd = nextStep ? javaDateAsMilliseconds(nextStep.createdAt) : (isJobInProgress ? now : getStepEndTime(step));
            active = !nextStep && isJobInProgress;
        }
        if (stepEnd !== null && stepEnd > end) end = stepEnd;
        stepEndTimesMap.set(step, {end: stepEnd, active});
    });

    return {start, end, stepEndTimesMap, historyForRunStepOnce, stepOrder};
};

const earliestEntryForRetry = (historyByStep, stepOrder, attemptId) =>
    stepOrder
        .flatMap((name) => historyByStep.get(name).filter((a) => a.attemptId === attemptId))
        .sort((a, b) => javaDateAsMicroseconds(a.startAt) - javaDateAsMicroseconds(b.startAt))[0];

const isStepSkippedForRetry = (historyByStep, stepOrder, attemptId, stepBase, stepsInAttempt) => {
    const history = historyByStep.get(stepBase);
    if (history.some((a) => a.attemptId === attemptId)) return false;
    const lastPriorAttempt = history.filter((a) => a.attemptId < attemptId).sort((a, b) => b.attemptId - a.attemptId)[0];
    if (!lastPriorAttempt?.succeeded) return false;
    return stepsInAttempt.some((name) => stepOrder.indexOf(name) > stepOrder.indexOf(stepBase));
};

const detectSkippedSteps = (historyByStep, stepOrder) => {
    const attemptIds = [...new Set([...historyByStep.values()].flat().map((a) => a.attemptId))].sort((a, b) => a - b);
    const skippedSteps = [];
    for (const attemptId of attemptIds.slice(1)) {
        const stepsInAttempt = stepOrder.filter((name) => historyByStep.get(name).some((a) => a.attemptId === attemptId));
        if (!stepsInAttempt.length) continue;
        const attemptStart = earliestEntryForRetry(historyByStep, stepOrder, attemptId);
        for (const stepBase of stepOrder) {
            if (!isStepSkippedForRetry(historyByStep, stepOrder, attemptId, stepBase, stepsInAttempt)) continue;
            skippedSteps.push({
                state: 'RUN_STEP_ONCE',
                stepName: `${stepBase}__${attemptId}`,
                attemptId,
                isSkipped: true,
                succeeded: true,
                createdAt: attemptStart.startAt
            });
        }
    }
    return skippedSteps;
};

export const convertStepsToTimeline = (steps, now) => {
    const {start, end, stepEndTimesMap, historyForRunStepOnce, stepOrder} = computeStepBounds(steps, now);
    return {start: Math.min(start, end), end: Math.max(start, end), stepEndTimesMap, skipped: detectSkippedSteps(historyForRunStepOnce, stepOrder)};
};

const groupSkippedStepsByRetry = (skipped) => {
    const skippedByRetry = new Map();
    skipped.forEach((step) => {
        if (!skippedByRetry.has(step.attemptId)) skippedByRetry.set(step.attemptId, []);
        skippedByRetry.get(step.attemptId).push(step);
    });
    return skippedByRetry;
};

export const addSkippedStepsToPerformedSteps = (steps, skipped) => {
    const pending = groupSkippedStepsByRetry(skipped);
    const merged = [];
    steps.forEach((step) => {
        if (step.state === 'RUN_STEP_ONCE' && step.stepName) {
            const attemptId = +step.stepName.split('__')[1];
            if (pending.has(attemptId)) {
                merged.push(...pending.get(attemptId));
                pending.delete(attemptId);
            }
        }
        merged.push(step);
    });
    return [...merged, ...[...pending.values()].flat()];
};

export const toTimelineSteps = (executionSteps) =>
    executionSteps.filter((step) => !EXCLUDED_NON_COMPACT.includes(step.state));

const compressorScale = (range, totalDuration, thresholdMs) => {
    const baseScale = Math.max(thresholdMs / 2, 1);
    const floor = 0.15 * baseScale;
    const duration = range.endMs - range.startMs;
    const share = duration / Math.max(totalDuration, 1);
    return Math.min(Math.max(baseScale * (1 - share), floor), baseScale);
};

const visualWidthRatio = (range, totalDuration, thresholdMs) => {
    const scale = compressorScale(range, totalDuration, thresholdMs);
    const duration = range.endMs - range.startMs;
    return (scale * Math.log(1 + duration / scale)) / duration;
};

export const createTimeCompressor = (longRanges, totalDuration, thresholdMs) => (timeMs) => {
    let compressedTimeSaved = 0;
    for (const r of longRanges) {
        if (timeMs <= r.startMs) break;
        const spanInGap = Math.min(timeMs, r.endMs) - r.startMs;
        compressedTimeSaved += spanInGap * (1 - visualWidthRatio(r, totalDuration, thresholdMs));
    }
    return timeMs - compressedTimeSaved;
};

const visualPercentage = (realMs, compressTime, compressedTimelineStart, compressedTimelineDuration) =>
    compressedTimelineDuration > 0 ? ((compressTime(realMs) - compressedTimelineStart) / compressedTimelineDuration) * 100 : 0;

const tickStepSeconds = (durationMs) => {
    const sec = durationMs / 1000;
    let stepSec = Math.round((sec / 4) / 5) * 5 || (sec < 5 ? 1 : 5);
    if (stepSec > 60) stepSec = Math.round(stepSec / 60) * 60;
    else if (stepSec > 20) stepSec = Math.round(stepSec / 15) * 15;
    return stepSec;
};

const buildBreakTicks = (longRanges, compressTime, timelineStartMs, compressedTimelineStart, compressedTimelineDuration) =>
    longRanges.map((r) => {
        const midRealMs = r.startMs + (r.endMs - r.startMs) / 2;
        const startRel = r.startMs - timelineStartMs;
        const endRel = r.endMs - timelineStartMs;
        const startLabel = startRel <= 0 ? '0' : `+${formatDuration(0, startRel)}`;
        return {
            ms: (startRel + endRel) / 2,
            pct: visualPercentage(midRealMs, compressTime, compressedTimelineStart, compressedTimelineDuration),
            label: `${startLabel} ... +${formatDuration(0, endRel)}`,
            isBreak: true, startMs: r.startMs, endMs: r.endMs,
        };
    });

const buildRegularTicks = (durationMs, stepSec, compressTime, timelineStartMs, compressedTimelineStart, compressedTimelineDuration, longRanges) => {
    const ticks = [];
    for (let ms = 0; ms <= durationMs; ms += stepSec * 1000) {
        const realTime = timelineStartMs + ms;
        if (longRanges.some(r => realTime > r.startMs && realTime < r.endMs)) continue;
        ticks.push({
            ms, startMs: realTime, isBreak: false,
            pct: visualPercentage(realTime, compressTime, compressedTimelineStart, compressedTimelineDuration),
            label: ms === 0 ? '0' : `+${formatDuration(0, ms)}`,
        });
    }
    return ticks;
};

const mergeTicks = (breakTicks, regularTicks) => {
    const candidates = [...breakTicks];
    regularTicks.forEach(normalTick => {
        if (!breakTicks.some(breakTick => Math.abs(breakTick.pct - normalTick.pct) < 8)) candidates.push(normalTick);
    });
    candidates.sort((a, b) => a.pct - b.pct);

    const ticks = [];
    for (const candidate of candidates) {
        const prev = ticks[ticks.length - 1];
        if (!prev || candidate.isBreak || prev.isBreak || (candidate.pct - prev.pct >= 6) || candidate.pct >= 98) ticks.push(candidate);
    }
    return ticks;
};

export const generateTimeTicks = (durationMs, compressTime, timelineStartMs, compressedTimelineDuration, longRanges = []) => {
    if (!durationMs || durationMs <= 0) return [{ms: 0, pct: 0, label: '0'}];
    const compressedTimelineStart = compressTime(timelineStartMs);
    const breakTicks = buildBreakTicks(longRanges, compressTime, timelineStartMs, compressedTimelineStart, compressedTimelineDuration);
    const regularTicks = buildRegularTicks(durationMs, tickStepSeconds(durationMs), compressTime, timelineStartMs, compressedTimelineStart, compressedTimelineDuration, longRanges);
    return mergeTicks(breakTicks, regularTicks);
};

const processOutcomeOfStep = (step, nextStep) => {
    if (step.state !== 'PROCESSING') return null;
    let outcome = null;
    if (nextStep?.state === 'FAILED' || step.succeeded === false) outcome = 'FAILED';
    if (nextStep?.state === 'SUCCEEDED' || step.succeeded === true) outcome = 'SUCCEEDED';
    return outcome;
};

const collectStepsIntoCompactRows = (executionSteps, stepEndMap, now) => {
    const rows = lifecycleRows();
    const stepMap = new Map();
    executionSteps.forEach((step, idx) => {
        const info = stepEndMap.get(step), startMs = javaDateAsMilliseconds(step.barStart ?? step.createdAt);
        const endMs = info?.end ?? (info?.active ? now : startMs);
        const nextStep = executionSteps.slice(idx + 1).find((s) => s.state !== 'RUN_STEP_ONCE');

        if (step.state === 'RUN_STEP_ONCE') {
            const name = getStepLabel(step);
            if (!stepMap.has(name)) stepMap.set(name, {key: name, label: name, isStep: true, items: []});
            stepMap.get(name).items.push({...step, startMs, endMs, active: info?.active, isSkipped: step.skipped || step.isSkipped});
        } else if (['SCHEDULED', 'ENQUEUED', 'PROCESSING'].includes(step.state)) {
            const row = rows.find(r => r.key === step.state);
            if (row) row.items.push({...step, startMs, endMs, active: info?.active, outcome: processOutcomeOfStep(step, nextStep)});
        }
    });
    return {rows, stepMap};
};

const addSkippedStepsToAllSteps = (stepMap, skipped) => {
    skipped.forEach((step) => {
        const name = getStepLabel(step);
        const startMs = javaDateAsMilliseconds(step.createdAt);
        if (!stepMap.has(name)) stepMap.set(name, {key: name, label: name, isStep: true, items: []});
        stepMap.get(name).items.push({...step, startMs, endMs: startMs, active: false, isSkipped: true});
    });
    return stepMap;
};

const computeCompactRowTotalMs = (rows, stepMap) =>
    [...rows.filter(r => r.items.length > 0), ...Array.from(stepMap.values())].map(row => ({
        ...row,
        totalMs: row.items.reduce((sum, item) => sum + Math.max(0, (item.endMs ?? item.startMs) - item.startMs), 0)
    }));

export const groupCompactStepsSequentially = (executionSteps, stepEndMap, now, skipped = []) => {
    const {rows, stepMap} = collectStepsIntoCompactRows(executionSteps, stepEndMap, now);
    addSkippedStepsToAllSteps(stepMap, skipped);
    return computeCompactRowTotalMs(rows, stepMap);
};

const computeBreakOffsetsWithinGanttBar = (compressRanges, itemStartMs, itemEndMs, compressTime, compressedBarStart, compressedBarEnd) =>
    compressRanges
        .filter(r => r.startMs >= itemStartMs && r.endMs <= itemEndMs)
        .map(r => {
            const compressedBarDuration = compressedBarEnd - compressedBarStart, compressedBreakMidpoint = compressTime(r.startMs + (r.endMs - r.startMs) / 2);
            return compressedBarDuration > 0 ? ((compressedBreakMidpoint - compressedBarStart) / compressedBarDuration) * 100 : 50;
        });

const createBarPlacements = ({compressTime, compressedTimelineStart, compressedTimelineDuration, compressRanges, reverse}) => (startMs, endMs, state) => {
    const itemStartMs = startMs;
    const itemEndMs = endMs ?? startMs;
    const compressedBarStart = compressTime(itemStartMs);
    const compressedBarEnd = compressTime(itemEndMs);
    const percentage = (compressedMs) => compressedTimelineDuration > 0 ? ((compressedMs - compressedTimelineStart) / compressedTimelineDuration) * 100 : 0;

    const offset = percentage(compressedBarStart);
    const calculatedWidth = percentage(compressedBarEnd) - offset;
    const isEndState = END_STATES.includes(state);
    const itemBreaks = computeBreakOffsetsWithinGanttBar(compressRanges, itemStartMs, itemEndMs, compressTime, compressedBarStart, compressedBarEnd);
    const isCompressed = itemBreaks.length > 0;
    const baseWidth = isEndState ? 0 : Math.max(calculatedWidth, isCompressed ? 3.0 : 0.3);
    return {
        offset: reverse ? 100 - offset - baseWidth : offset,
        width: baseWidth,
        isPoint: isEndState,
        isCompressed,
        breakOffsets: reverse ? itemBreaks.map((b) => 100 - b) : itemBreaks,
    };
};

const collectTimestamps = (rawSteps, stepEndMap, start, end, now) => {
    const timestamps = new Set([start, end]);
    rawSteps.forEach((step) => {
        const startMs = javaDateAsMilliseconds(step.barStart ?? step.createdAt);
        const info = stepEndMap.get(step);
        const endMs = info?.end ?? (info?.active ? now : startMs);
        if (Number.isFinite(startMs)) timestamps.add(startMs);
        if (Number.isFinite(endMs)) timestamps.add(endMs);
    });
    return timestamps;
};

const detectSpansThatShouldNotCompress = (rawSteps, stepEndMap) => {
    const protectedSpans = [];
    rawSteps.forEach((step, i) => {
        if (step.state !== 'PROCESSING') return;
        let hasSubStep = false;
        for (let j = i + 1; j < rawSteps.length && rawSteps[j].state === 'RUN_STEP_ONCE'; j++) hasSubStep = true;
        if (!hasSubStep) return;
        const processingStart = javaDateAsMilliseconds(step.createdAt);
        const processingEnd = stepEndMap.get(step)?.end ?? processingStart;
        if (processingEnd > processingStart) protectedSpans.push({startMs: processingStart, endMs: processingEnd});
    });
    return protectedSpans;
};

const detectLongRangesToCompress = (rawSteps, stepEndMap, start, end, now, compressionThresholdMs) => {
    const sortedTimestamps = Array.from(collectTimestamps(rawSteps, stepEndMap, start, end, now)).sort((a, b) => a - b);
    const protectedSpans = detectSpansThatShouldNotCompress(rawSteps, stepEndMap);
    const longRanges = [];
    for (let i = 0; i < sortedTimestamps.length - 1; i++) {
        const segmentStart = sortedTimestamps[i], segmentEnd = sortedTimestamps[i + 1];
        if (segmentEnd - segmentStart <= compressionThresholdMs) continue;
        if (protectedSpans.some((p) => segmentStart >= p.startMs && segmentEnd <= p.endMs)) continue;
        longRanges.push({startMs: segmentStart, endMs: segmentEnd});
    }
    return longRanges;
};

const buildCompactRetryEvents = (rawSteps, compressTime, compressedTimelineStart, compressedTimelineDuration) => {
    const events = [];
    let count = 0;
    rawSteps.forEach((step, idx) => {
        if (idx > 0 && step.state === 'SCHEDULED') {
            count += 1;
            const retryMs = javaDateAsMilliseconds(step.barStart ?? step.createdAt);
            const pct = compressedTimelineDuration > 0 ? ((compressTime(retryMs) - compressedTimelineStart) / compressedTimelineDuration) * 100 : 0;
            events.push({count, pct, ms: retryMs});
        }
    });
    return events;
};

const applyPlacement = (item, getPlacement) => {
    const {offset, width, isPoint, isCompressed, breakOffsets} = getPlacement(item.startMs, item.endMs, item.state);
    return {...item, placement: {offset, width, isPoint, isCompressed, breakOffsets}};
};

const buildCompactRows = (rawSteps, stepEndMap, now, skipped, getPlacement, reverse) => {
    const groupedRows = groupCompactStepsSequentially(rawSteps, stepEndMap, now, skipped);
    const ordered = reverse ? groupedRows.slice().reverse() : groupedRows;
    return ordered.map((row) => ({...row, items: row.items.map((item) => applyPlacement(item, getPlacement))}));
};

const buildDetailedRows = (detailedSteps, stepEndMap, getPlacement, reverse) => {
    let chronologicalRetry = 0;
    const detailedRows = detailedSteps.map((step, index) => {
        const info = stepEndMap.get(step);
        const stepStartMs = javaDateAsMilliseconds(step.barStart ?? step.createdAt);
        const stepEndMs = info?.end ?? null;
        const active = info?.active ?? false;
        const isRetry = index > 0 && step.state === 'SCHEDULED';
        if (isRetry) chronologicalRetry += 1;
        return {
            step,
            item: {
                ...step,
                startMs: stepStartMs,
                endMs: stepEndMs,
                active,
                isSkipped: step.isSkipped,
                placement: getPlacement(stepStartMs, stepEndMs, step.state)
            },
            label: getStepLabel(step),
            isStep: step.state === 'RUN_STEP_ONCE',
            isRetry,
            retryNumber: chronologicalRetry,
        };
    });
    return reverse ? detailedRows.slice().reverse() : detailedRows;
};

export const buildTimelineModel = ({steps, mode, compression, reverse, now}) => {
    const rawSteps = (steps ?? []).filter((step) => !EXCLUDED_NON_COMPACT.includes(step.state));
    if (rawSteps.length === 0) return null;

    const {start, end, stepEndTimesMap, skipped} = convertStepsToTimeline(rawSteps, now);
    const baseDetailed = toTimelineSteps(steps ?? []);
    const detailedSteps = skipped.length && mode !== 'compact' ? addSkippedStepsToPerformedSteps(baseDetailed, skipped) : baseDetailed;
    const duration = end - start;
    const compressionThresholdMs = Math.max(MIN_COMPRESSION_THRESHOLD_MS, duration * COMPRESSION_THRESHOLD);

    const longRanges = detectLongRangesToCompress(rawSteps, stepEndTimesMap, start, end, now, compressionThresholdMs);
    const compressRanges = compression === 'actual' ? [] : longRanges;
    const compressTime = createTimeCompressor(compressRanges, duration, compressionThresholdMs);
    const compressedTimelineStart = compressTime(start);
    const compressedTimelineEnd = compressTime(end);
    const compressedTimelineDuration = compressedTimelineEnd - compressedTimelineStart;
    const getPlacement = createBarPlacements({
        compressTime, compressedTimelineStart, compressedTimelineDuration, compressRanges, reverse
    });

    return {
        start, end, duration,
        ticks: generateTimeTicks(duration, compressTime, start, compressedTimelineDuration, compressRanges),
        retryEvents: buildCompactRetryEvents(rawSteps, compressTime, compressedTimelineStart, compressedTimelineDuration),
        compactRows: mode === 'compact' ? buildCompactRows(rawSteps, stepEndTimesMap, now, skipped, getPlacement, reverse) : [],
        orderedDetailedRows: buildDetailedRows(detailedSteps, stepEndTimesMap, getPlacement, reverse),
    };
};
