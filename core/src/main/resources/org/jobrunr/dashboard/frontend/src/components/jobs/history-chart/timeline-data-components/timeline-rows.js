import {dateAsMilliseconds} from "../../../../utils/helper-functions.js";
import {ENQUEUED, FAILED, PROCESSING, RUN_STEP_ONCE, SCHEDULED, STATE_LABELS, SUCCEEDED} from "../../../utils/state-names.js";
import {addSkippedStepsToAllSteps} from "./determine-skipped-steps.js";

const lifecycleRows = () => [
    {key: SCHEDULED, label: STATE_LABELS[SCHEDULED], isStep: false, items: []},
    {key: ENQUEUED, label: STATE_LABELS[ENQUEUED], isStep: false, items: []},
    {key: PROCESSING, label: STATE_LABELS[PROCESSING], isStep: false, items: []},
];

export const getStepLabel = (step) => {
    if (step.isConsolidated) return 'Execution time';
    if (step.state === RUN_STEP_ONCE && step.stepName) return step.stepName.split('__')[0];
    return STATE_LABELS[step.state] ?? step.state ?? 'Unknown';
};

const processOutcomeOfStep = (step, nextStep) => {
    if (step.state !== PROCESSING) return null;
    let outcome = null;
    if (nextStep?.state === FAILED || step.succeeded === false) outcome = FAILED;
    if (nextStep?.state === SUCCEEDED || step.succeeded === true) outcome = SUCCEEDED;
    return outcome;
};

const getOrCreateStepRow = (stepMap, name) => {
    if (!stepMap.has(name)) stepMap.set(name, {key: name, label: name, isStep: true, items: []});
    return stepMap.get(name);
};

const addRunStepOnceRow = (stepMap, step, startMs, endMs, info) => {
    getOrCreateStepRow(stepMap, getStepLabel(step))
        .items.push({...step, startMs, endMs, active: info?.active, isSkipped: step.skipped || step.isSkipped});
};

const addLifecycleRow = (rows, step, startMs, endMs, info, nextStep) => {
    const row = rows.find(r => r.key === step.state);
    if (row) row.items.push({...step, startMs, endMs, active: info?.active, outcome: processOutcomeOfStep(step, nextStep)});
};

const collectStepsIntoCompactRows = (executionSteps, stepEndMap, now) => {
    const rows = lifecycleRows();
    const stepMap = new Map();
    executionSteps.forEach((step, idx) => {
        const info = stepEndMap.get(step);
        const startMs = dateAsMilliseconds(step.barStart ?? step.createdAt);
        const endMs = info?.end ?? (info?.active ? now : startMs);
        const nextStep = executionSteps.slice(idx + 1).find((s) => s.state !== RUN_STEP_ONCE);
        if (step.state === RUN_STEP_ONCE) addRunStepOnceRow(stepMap, step, startMs, endMs, info);
        else if ([SCHEDULED, ENQUEUED, PROCESSING].includes(step.state)) addLifecycleRow(rows, step, startMs, endMs, info, nextStep);
    });
    return {rows, stepMap};
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

const applyPlacement = (item, getPlacement) => {
    const {offset, width, isPoint, isCompressed, breakOffsets} = getPlacement(item.startMs, item.endMs, item.state);
    return {...item, placement: {offset, width, isPoint, isCompressed, breakOffsets}};
};

export const buildCompactRows = (rawSteps, stepEndMap, now, skipped, getPlacement, reverse) => {
    const groupedRows = groupCompactStepsSequentially(rawSteps, stepEndMap, now, skipped);
    const ordered = reverse ? groupedRows.slice().reverse() : groupedRows;
    return ordered.map((row) => ({...row, items: row.items.map((item) => applyPlacement(item, getPlacement))}));
};

export const buildCompactRetryEvents = (rawSteps, axis) => {
    const events = [];
    let count = 0;
    rawSteps.forEach((step, idx) => {
        if (idx > 0 && step.state === SCHEDULED) {
            count += 1;
            const retryMs = dateAsMilliseconds(step.barStart ?? step.createdAt);
            events.push({count, pct: axis.percentage(retryMs), ms: retryMs});
        }
    });
    return events;
};

const buildDetailedItem = (step, info, getPlacement) => {
    const stepStartMs = dateAsMilliseconds(step.barStart ?? step.createdAt);
    const stepEndMs = info?.end ?? null;
    return {
        ...step,
        startMs: stepStartMs,
        endMs: stepEndMs,
        active: info?.active ?? false,
        isSkipped: step.isSkipped,
        placement: getPlacement(stepStartMs, stepEndMs, step.state)
    };
};

export const buildDetailedRows = (detailedSteps, stepEndMap, getPlacement, reverse) => {
    let chronologicalRetry = 0;
    const detailedRows = detailedSteps.map((step, index) => {
        const isRetry = index > 0 && step.state === SCHEDULED;
        if (isRetry) chronologicalRetry += 1;
        return {
            step,
            item: buildDetailedItem(step, stepEndMap.get(step), getPlacement),
            label: getStepLabel(step),
            isStep: step.state === RUN_STEP_ONCE,
            isRetry,
            retryNumber: chronologicalRetry,
        };
    });
    return reverse ? detailedRows.slice().reverse() : detailedRows;
};
