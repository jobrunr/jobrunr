import {comparePreciseDates, dateAsMilliseconds} from "../../../../utils/helper-functions.js";
import {RUN_STEP_ONCE} from "../../../utils/state-names.js";

const earliestEntryForRetry = (historyByStep, stepOrder, attemptId) =>
    stepOrder
        .flatMap((name) => historyByStep.get(name).filter((a) => a.attemptId === attemptId))
        .sort((a, b) => comparePreciseDates(a.startAt, b.startAt))[0];

// Skipped = absent this attempt, succeeded in a prior attempt, and a later step
// in canonical order did run in this attempt, so execution passed this step rather than merely not having reached it yet.
const isStepSkippedForRetry = (historyByStep, stepOrder, attemptId, stepBase, stepsInAttempt) => {
    const history = historyByStep.get(stepBase);
    if (history.some((a) => a.attemptId === attemptId)) return false;
    const lastPriorAttempt = history.filter((a) => a.attemptId < attemptId).sort((a, b) => b.attemptId - a.attemptId)[0];
    if (!lastPriorAttempt?.succeeded) return false;
    return stepsInAttempt.some((name) => stepOrder.indexOf(name) > stepOrder.indexOf(stepBase));
};

const findAttemptContext = (historyByStep, stepOrder, attemptId) => {
    const stepsInAttempt = stepOrder.filter((name) => historyByStep.get(name).some((a) => a.attemptId === attemptId));
    if (!stepsInAttempt.length) return null;
    return {stepsInAttempt, attemptStart: earliestEntryForRetry(historyByStep, stepOrder, attemptId)};
};

const buildSkippedStep = (stepBase, attemptId, attemptStart) => ({
    state: RUN_STEP_ONCE,
    stepName: `${stepBase}__${attemptId}`,
    stepBase,
    attemptId,
    isSkipped: true,
    succeeded: true,
    createdAt: attemptStart.startAt
});

export const detectSkippedSteps = (historyByStep, stepOrder) => {
    const attemptIds = [...new Set([...historyByStep.values()].flat().map((a) => a.attemptId))].sort((a, b) => a - b);
    const skippedSteps = [];
    for (const attemptId of attemptIds.slice(1)) {
        const context = findAttemptContext(historyByStep, stepOrder, attemptId);
        if (!context) continue;
        const {stepsInAttempt, attemptStart} = context;
        for (const stepBase of stepOrder) {
            if (!isStepSkippedForRetry(historyByStep, stepOrder, attemptId, stepBase, stepsInAttempt)) continue;
            skippedSteps.push(buildSkippedStep(stepBase, attemptId, attemptStart));
        }
    }
    return skippedSteps;
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
        if (step.state === RUN_STEP_ONCE && step.stepName) {
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

export const addSkippedStepsToAllSteps = (stepMap, skipped) => {
    skipped.forEach((step) => {
        const name = step.stepBase;
        const startMs = dateAsMilliseconds(step.createdAt);
        if (!stepMap.has(name)) stepMap.set(name, {key: name, label: name, isStep: true, items: []});
        stepMap.get(name).items.push({...step, startMs, endMs: startMs, active: false, isSkipped: true});
    });
    return stepMap;
};
