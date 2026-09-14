import {dateAsMilliseconds} from "../../../../utils/helper-functions.js";
import {AWAITING, DELETED, SCHEDULED} from "../../../utils/state-names.js";
import {ENTRY_TYPES, RUN_STEP_ONCE} from "../utils/timeline-entries.js";
import {createBarPlacements, createCompressedAxis, createTimeCompressor, detectLongRangesToCompress, generateTimeTicks} from "./timeline-axis.js";
import {buildCompactRetryEvents, buildCompactRows, buildDetailedRows} from "./timeline-rows.js";

export {getStepLabel, groupCompactStepsSequentially} from "./timeline-rows.js";
export {createTimeCompressor, generateTimeTicks};

export const TIMELINE_MODES = {
    COMPACT: "compact",
    DETAILED: "detailed",
}

export const TIMELINE_COMPRESSION_MODES = {
    LINEAR: "linear",
    COMPRESSED: "compressed",
}

export const EXCLUDED_STATES = [AWAITING, DELETED];

const MIN_COMPRESSION_THRESHOLD_MS = 60000;
const COMPRESSION_THRESHOLD = 0.15;

export const removeInitialScheduled = (steps) => {
    const list = steps ?? [];
    return list.length > 0 && list[0].state === SCHEDULED ? list.slice(1) : list;
};

// why: the timeline entries use ISO instants (startedAt/finishedAt) while the gantt chart works with milliseconds;
// unfinished entries are still active and keep running until now
const toTimelineItems = (entries, now) => entries.map((entry) => {
    const active = !entry.finishedAt;
    const isPoint = entry.type === ENTRY_TYPES.milestone;
    return {
        ...entry,
        startMs: dateAsMilliseconds(entry.startedAt),
        endMs: active ? now : dateAsMilliseconds(entry.finishedAt),
        active,
        isPoint,
        isSkipped: isPoint && entry.state === RUN_STEP_ONCE,
    };
});

export const buildTimelineModel = ({steps, timelineMode, compressionMode, reverse, now}) => {
    if (!steps || steps.length === 0) return null;

    const items = toTimelineItems(steps, now);
    const start = items[0].startMs;
    const end = Math.max(items[items.length - 1].endMs, start);
    const duration = end - start;
    const compressionThresholdMs = Math.max(MIN_COMPRESSION_THRESHOLD_MS, duration * COMPRESSION_THRESHOLD);

    const longRanges = detectLongRangesToCompress(items, start, end, compressionThresholdMs);
    const compressRanges = compressionMode === TIMELINE_COMPRESSION_MODES.LINEAR ? [] : longRanges;
    const compressTime = createTimeCompressor(compressRanges, duration, compressionThresholdMs);
    const compressedTimelineStart = compressTime(start);
    const compressedTimelineDuration = compressTime(end) - compressedTimelineStart;
    const axis = createCompressedAxis(compressTime, compressedTimelineStart, compressedTimelineDuration);
    const getPlacement = createBarPlacements({axis, compressRanges, reverse});

    return {
        start,
        end,
        duration,
        ticks: generateTimeTicks(duration, compressTime, start, compressedTimelineDuration, compressRanges),
        retryEvents: buildCompactRetryEvents(items, axis),
        compactRows: timelineMode === TIMELINE_MODES.COMPACT ? buildCompactRows(items, getPlacement, reverse) : [],
        orderedDetailedRows: buildDetailedRows(items, getPlacement, reverse),
    };
};
