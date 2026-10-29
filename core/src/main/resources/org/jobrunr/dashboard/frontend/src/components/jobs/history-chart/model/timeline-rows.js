import {ENQUEUED, PROCESSING, SCHEDULED} from "../../../utils/state-names.js";
import {REQUEUE_STEP, RETRY_STEP, RUN_STEP_ONCE, TIMELINE_ENTRY_LABELS} from "./timeline-entries.js";

const LIFECYCLE_STATES = [SCHEDULED, ENQUEUED, PROCESSING];

const lifecycleRows = () => LIFECYCLE_STATES.map((state) => ({key: state, label: TIMELINE_ENTRY_LABELS[state], isStep: false, items: []}));

export const getStepLabel = (step) => step.label ?? TIMELINE_ENTRY_LABELS[step.state] ?? step.state ?? 'Unknown';

const getOrCreateStepRow = (stepMap, name) => {
    if (!stepMap.has(name)) stepMap.set(name, {key: name, label: name, isStep: true, items: []});
    return stepMap.get(name);
};

const isRequeueMarker = (item) => item.state === REQUEUE_STEP;

const isRetryOrRequeueMarker = (item) => item.state === RETRY_STEP || item.state === REQUEUE_STEP;

const collectStepsIntoCompactRows = (items) => {
    const rows = lifecycleRows();
    const stepMap = new Map();
    items.forEach((item) => {
        if (item.state === RUN_STEP_ONCE) getOrCreateStepRow(stepMap, item.label).items.push(item);
        else if (LIFECYCLE_STATES.includes(item.state)) rows.find((row) => row.key === item.state).items.push(item);
    });
    return {rows, stepMap};
};

const computeCompactRowTotalMs = (rows, stepMap) =>
    [...rows.filter((row) => row.items.length > 0), ...Array.from(stepMap.values())].map((row) => ({
        ...row,
        totalMs: row.items.reduce((sum, item) => sum + Math.max(0, item.endMs - item.startMs), 0),
    }));

export const groupCompactStepsSequentially = (items) => {
    const {rows, stepMap} = collectStepsIntoCompactRows(items);
    return computeCompactRowTotalMs(rows, stepMap);
};

const applyPlacement = (item, getPlacement) => {
    const {offset, width, isPoint, isCompressed, breakOffsets} = getPlacement(item);
    return {...item, placement: {offset, width, isPoint, isCompressed, breakOffsets}};
};

export const buildCompactRows = (items, getPlacement, reverse) => {
    const groupedRows = groupCompactStepsSequentially(items);
    const ordered = reverse ? groupedRows.slice().reverse() : groupedRows;
    return ordered.map((row) => ({...row, items: row.items.map((item) => applyPlacement(item, getPlacement))}));
};

export const buildCompactRetryEvents = (items, axis) =>
    items.filter(isRetryOrRequeueMarker).map((item, index) => ({
        count: index + 1,
        label: item.label,
        isRequeue: isRequeueMarker(item),
        pct: axis.percentage(item.startMs),
        ms: item.startMs,
    }));

export const buildDetailedRows = (items, getPlacement, reverse) => {
    const detailedRows = items.map((item) => {
        if (isRetryOrRequeueMarker(item)) return {item, label: item.label, isSeparator: true, isRequeue: isRequeueMarker(item)};
        // why: in detailed mode the outcome of an attempt is shown by the FAILED/SUCCEEDED milestone row that follows it, not by a marker on the processing bar
        const {outcome, ...itemWithoutOutcome} = item;
        return {
            item: applyPlacement(itemWithoutOutcome, getPlacement),
            label: getStepLabel(item),
            isStep: item.state === RUN_STEP_ONCE,
        };
    });
    return reverse ? detailedRows.slice().reverse() : detailedRows;
};
