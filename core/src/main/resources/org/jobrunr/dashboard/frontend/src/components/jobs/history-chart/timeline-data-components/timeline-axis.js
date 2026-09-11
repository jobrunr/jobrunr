import {dateAsMilliseconds, formatDuration} from "../../../../utils/helper-functions.js";
import {END_STATES, PROCESSING, RUN_STEP_ONCE} from "../../../utils/state-names.js";

const COMPRESSOR_FLOOR_RATIO = 0.15;
const TICK_MERGE_PCT = 8;
const MIN_TICK_GAP_PCT = 6;
const LAST_TICK_KEEP_PCT = 98;
const MIN_BAR_WIDTH = 0.3;
const MIN_COMPRESSED_BAR_WIDTH = 3.0;
const DEFAULT_BREAK_OFFSET_PCT = 50;
const TARGET_TICK_COUNT = 4;
const FINE_BUCKET_SEC = 5;
const MEDIUM_BUCKET_SEC = 15;
const MEDIUM_BUCKET_THRESHOLD_SEC = 20;
const COARSE_BUCKET_SEC = 60;

const roundToNearest = (value, bucket) => Math.round(value / bucket) * bucket;

// Scale factor for how much a gap shrinks: a gap taking a larger share of the total
// duration shrinks more, floored so a gap never collapses to zero width.
const compressorScale = (range, totalDuration, thresholdMs) => {
    const baseScale = Math.max(thresholdMs / 2, 1);
    const floor = COMPRESSOR_FLOOR_RATIO * baseScale;
    const duration = range.endMs - range.startMs;
    const share = duration / Math.max(totalDuration, 1);
    return Math.min(Math.max(baseScale * (1 - share), floor), baseScale);
};

// Fraction of a gap's real duration that stays visible after compression.
// A log curve — small gaps stay ~full, large gaps shrink proportionally rather
// than vanishing. Drives how much time `createTimeCompressor` subtracts per gap.
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

export const createCompressedAxis = (compressTime, start, duration) => ({
    compressTime,
    percentage: (realMs) => duration > 0 ? ((compressTime(realMs) - start) / duration) * 100 : 0,
});

const tickStepSeconds = (durationMs) => {
    const totalSec = durationMs / 1000;
    let stepSec = roundToNearest(totalSec / TARGET_TICK_COUNT, FINE_BUCKET_SEC) || (totalSec < FINE_BUCKET_SEC ? 1 : FINE_BUCKET_SEC);
    if (stepSec > COARSE_BUCKET_SEC) return roundToNearest(stepSec, COARSE_BUCKET_SEC);
    if (stepSec > MEDIUM_BUCKET_THRESHOLD_SEC) return roundToNearest(stepSec, MEDIUM_BUCKET_SEC);
    return stepSec;
};

const buildBreakTicks = (longRanges, axis, timelineStartMs) =>
    longRanges.map((r) => {
        const midRealMs = r.startMs + (r.endMs - r.startMs) / 2;
        const startRel = r.startMs - timelineStartMs;
        const endRel = r.endMs - timelineStartMs;
        const startLabel = startRel <= 0 ? '0' : `+${formatDuration(0, startRel)}`;
        return {
            ms: (startRel + endRel) / 2,
            pct: axis.percentage(midRealMs),
            label: `${startLabel} ... +${formatDuration(0, endRel, 2)}`,
            isBreak: true, startMs: r.startMs, endMs: r.endMs,
        };
    });

const buildRegularTicks = (durationMs, stepSec, axis, timelineStartMs, longRanges) => {
    const ticks = [];
    for (let ms = 0; ms <= durationMs; ms += stepSec * 1000) {
        const realTime = timelineStartMs + ms;
        if (longRanges.some(r => realTime > r.startMs && realTime < r.endMs)) continue;
        ticks.push({
            ms, startMs: realTime, isBreak: false,
            pct: axis.percentage(realTime),
            label: ms === 0 ? '0' : `+${formatDuration(0, ms, 2)}`,
        });
    }
    return ticks;
};

const shouldKeepTick = (candidate, prev) =>
    !prev || candidate.isBreak || prev.isBreak || (candidate.pct - prev.pct >= MIN_TICK_GAP_PCT) || candidate.pct >= LAST_TICK_KEEP_PCT;

const mergeTicks = (breakTicks, regularTicks) => {
    const candidates = [...breakTicks];
    regularTicks.forEach(normalTick => {
        if (!breakTicks.some(breakTick => Math.abs(breakTick.pct - normalTick.pct) < TICK_MERGE_PCT)) candidates.push(normalTick);
    });
    candidates.sort((a, b) => a.pct - b.pct);

    const ticks = [];
    for (const candidate of candidates) {
        if (shouldKeepTick(candidate, ticks[ticks.length - 1])) ticks.push(candidate);
    }
    return ticks;
};

export const generateTimeTicks = (durationMs, compressTime, timelineStartMs, compressedTimelineDuration, longRanges = []) => {
    if (!durationMs || durationMs <= 0) return [{ms: 0, pct: 0, label: '0'}];
    const axis = createCompressedAxis(compressTime, compressTime(timelineStartMs), compressedTimelineDuration);
    const breakTicks = buildBreakTicks(longRanges, axis, timelineStartMs);
    const regularTicks = buildRegularTicks(durationMs, tickStepSeconds(durationMs), axis, timelineStartMs, longRanges);
    return mergeTicks(breakTicks, regularTicks);
};

const collectTimestamps = (rawSteps, stepEndMap, start, end, now) => {
    const timestamps = new Set([start, end]);
    rawSteps.forEach((step) => {
        const startMs = dateAsMilliseconds(step.barStart ?? step.createdAt);
        const info = stepEndMap.get(step);
        const endMs = info?.end ?? (info?.active ? now : startMs);
        if (Number.isFinite(startMs)) timestamps.add(startMs);
        if (Number.isFinite(endMs)) timestamps.add(endMs);
    });
    return timestamps;
};

const hasFollowingRunStepOnce = (rawSteps, i) =>
    i + 1 < rawSteps.length && rawSteps[i + 1].state === RUN_STEP_ONCE;

const detectSpansThatShouldNotCompress = (rawSteps, stepEndMap) => {
    const protectedSpans = [];
    rawSteps.forEach((step, i) => {
        if (step.state !== PROCESSING || !hasFollowingRunStepOnce(rawSteps, i)) return;
        const processingStart = dateAsMilliseconds(step.createdAt);
        const processingEnd = stepEndMap.get(step)?.end ?? processingStart;
        if (processingEnd > processingStart) protectedSpans.push({startMs: processingStart, endMs: processingEnd});
    });
    return protectedSpans;
};

export const detectLongRangesToCompress = (rawSteps, stepEndMap, start, end, now, compressionThresholdMs) => {
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

const computeBreakOffsetsWithinGanttBar = (compressRanges, itemStartMs, itemEndMs, compressTime, compressedBarStart, compressedBarEnd) =>
    compressRanges
        .filter(r => r.startMs >= itemStartMs && r.endMs <= itemEndMs)
        .map(r => {
            const compressedBarDuration = compressedBarEnd - compressedBarStart;
            const compressedBreakMidpoint = compressTime(r.startMs + (r.endMs - r.startMs) / 2);
            return compressedBarDuration > 0 ? ((compressedBreakMidpoint - compressedBarStart) / compressedBarDuration) * 100 : DEFAULT_BREAK_OFFSET_PCT;
        });

export const createBarPlacements = ({axis, compressRanges, reverse}) => (startMs, endMs, state) => {
    const itemStartMs = startMs;
    const itemEndMs = endMs ?? startMs;
    const compressedBarStart = axis.compressTime(itemStartMs);
    const compressedBarEnd = axis.compressTime(itemEndMs);
    const offset = axis.percentage(itemStartMs);
    const calculatedWidth = axis.percentage(itemEndMs) - offset;
    const isEndState = END_STATES.includes(state);
    const itemBreaks = computeBreakOffsetsWithinGanttBar(compressRanges, itemStartMs, itemEndMs, axis.compressTime, compressedBarStart, compressedBarEnd);
    const isCompressed = itemBreaks.length > 0;
    const baseWidth = isEndState ? 0 : Math.max(calculatedWidth, isCompressed ? MIN_COMPRESSED_BAR_WIDTH : MIN_BAR_WIDTH);
    return {
        offset: reverse ? 100 - offset - baseWidth : offset,
        width: baseWidth,
        isPoint: isEndState,
        isCompressed,
        breakOffsets: reverse ? itemBreaks.map((b) => 100 - b) : itemBreaks,
    };
};
