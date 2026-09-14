import {compareInstants} from "../../../../utils/helper-functions.js";
import {DELETED, END_STATES, ENQUEUED, FAILED, PROCESSING, SCHEDULED, STATE_LABELS, SUCCEEDED} from "../../../utils/state-names.js";

export const RUN_STEP_ONCE = "RUN_STEP_ONCE";
export const RETRY_STEP = "RETRY";
export const REQUEUE_STEP = "REQUEUE";

export const ENTRY_TYPES = {
    span: "span",
    milestone: "milestone",
}

const JOBRUNR_STEP_PREFIX = "jr_step_";
const JOBRUNR_STEP_START_PREFIX = JOBRUNR_STEP_PREFIX + "start_";
const JOBRUNR_STEP_END_PREFIX = JOBRUNR_STEP_PREFIX + "end_";
const JOBRUNR_STEP_RESULT_PREFIX = JOBRUNR_STEP_PREFIX + "result_";
const JOBRUNR_STEP_RESULT_CLASS_PREFIX = JOBRUNR_STEP_RESULT_PREFIX + "class_";

const getOrCreateStep = (steps, name) => (steps[name] ??= {});

const formatProcessingSteps = (jobMetadata) => {
    const steps = {};
    const results = new Map();
    for (const [key, value] of Object.entries(jobMetadata)) {
        if (key.startsWith(JOBRUNR_STEP_RESULT_CLASS_PREFIX)) continue;
        else if (key.startsWith(JOBRUNR_STEP_RESULT_PREFIX)) results.set(key.slice(JOBRUNR_STEP_RESULT_PREFIX.length), value);
        else if (key.startsWith(JOBRUNR_STEP_START_PREFIX)) getOrCreateStep(steps, key.slice(JOBRUNR_STEP_START_PREFIX.length)).startedAt = value;
        else if (key.startsWith(JOBRUNR_STEP_END_PREFIX)) getOrCreateStep(steps, key.slice(JOBRUNR_STEP_END_PREFIX.length)).finishedAt = value;
        else if (key.startsWith(JOBRUNR_STEP_PREFIX)) getOrCreateStep(steps, key.slice(JOBRUNR_STEP_PREFIX.length)).succeeded = value;
    }

    return Object.entries(steps)
        .map(([key, value]) => {
            const name = key.split('__')[0];
            return {
                ...value,
                state: RUN_STEP_ONCE,
                label: name,
                result: value.succeeded ? results.get(name) : undefined,
            };
        })
        .sort((a, b) => compareInstants(a.startedAt, b.startedAt));
}

const getStepsStartedInAttempt = (processingState, remainingSteps) => {
    if (!processingState.finishedAt) return remainingSteps; // why: if the processing state is ongoing, all remaining steps belong to this attempt
    const firstStepOutsideAttemptIndex = remainingSteps.findIndex((step) => compareInstants(step.startedAt, processingState.finishedAt) > 0);
    return firstStepOutsideAttemptIndex === -1
        ? remainingSteps
        : remainingSteps.slice(0, firstStepOutsideAttemptIndex);
}

const createSkippedStep = (processingState, step) => {
    // why: the step succeeded in an earlier attempt, so runStepOnce skips it in this attempt - shown as a milestone at the start of the attempt
    return {...step, type: ENTRY_TYPES.milestone, startedAt: processingState.startedAt, finishedAt: processingState.startedAt};
}

const getStepsForProcessingState = (processingState, steps, stepsCursor, succeededSteps) => {
    const stepsInAttempt = getStepsStartedInAttempt(processingState, steps.slice(stepsCursor));

    return {
        entries: [
            ...succeededSteps.map((step) => createSkippedStep(processingState, step)),
            ...stepsInAttempt.map((step) => ({
                ...step,
                type: ENTRY_TYPES.span,
                finishedAt: step.finishedAt ?? processingState.finishedAt, // why: in case a job is orphaned, a step may not have an end time, it is set to the end of the processing state it started in
            })),
        ],
        stepsCursor: stepsCursor + stepsInAttempt.length,
        succeededSteps: [...succeededSteps, ...stepsInAttempt.filter((step) => step.succeeded)],
    };
}

const getStateFinishedAt = (jobState, nextJobState) => {
    if (jobState.state === DELETED) return jobState.deleteAt;
    if (jobState.state === SUCCEEDED) return jobState.createdAt;
    if (jobState.state === FAILED) return jobState.createdAt;
    return nextJobState?.createdAt;
}

const isEndOrDeletedState = (state) => [...END_STATES, DELETED].includes(state);

const getTimelineType = (state) => isEndOrDeletedState(state) ? ENTRY_TYPES.milestone : ENTRY_TYPES.span;

const isRetried = (jobState, nextJobState) => {
    return jobState.state === FAILED && nextJobState?.state === SCHEDULED;
}

const isRequeued = (jobState, nextJobState) => {
    return isEndOrDeletedState(jobState.state) && nextJobState?.state === ENQUEUED;
}

const createRetryStep = (nextJobState, retryNumber) => {
    return {
        state: RETRY_STEP,
        label: `Retry ${retryNumber}`,
        type: ENTRY_TYPES.milestone,
        startedAt: nextJobState.startedAt,
        finishedAt: nextJobState.startedAt
    };
}

const createRequeueStep = (nextJobState, requeueNumber) => {
    return {
        state: REQUEUE_STEP,
        label: `Requeue ${requeueNumber}`,
        type: ENTRY_TYPES.milestone,
        startedAt: nextJobState.startedAt,
        finishedAt: nextJobState.startedAt
    };
}

const getProcessingOutcome = (jobState, nextJobState) => {
    if (jobState.state !== PROCESSING) return null;
    if (nextJobState?.state === FAILED) return FAILED;
    if (nextJobState?.state === SUCCEEDED) return SUCCEEDED;
    return null;
}

const formatJobStates = (jobHistory) => {
    return jobHistory.map((jobState, index) => {
        const nextJobState = jobHistory[index + 1];
        return {
            state: jobState.state,
            label: STATE_LABELS[jobState.state],
            startedAt: jobState.createdAt,
            finishedAt: getStateFinishedAt(jobState, nextJobState),
            type: getTimelineType(jobState.state),
            outcome: getProcessingOutcome(jobState, nextJobState),
        };
    });
}

export const createJobExecutionTimelineEntries = (job) => {
    const formattedJobStates = formatJobStates(job.jobHistory);
    const formattedProcessingSteps = formatProcessingSteps(job.metadata);

    // the formatted steps are sorted by startedAt, so each processing state consumes the steps from the stepsCursor onwards
    let stepsCursor = 0;
    let succeededSteps = []; // steps that succeeded in an earlier attempt, to copy over as milestones in each following attempt
    let retryCount = 0;
    let requeueCount = 0;

    return formattedJobStates.flatMap((jobState, index) => {
        const entries = [jobState];
        if (jobState.state === PROCESSING) {
            const processingSteps = getStepsForProcessingState(jobState, formattedProcessingSteps, stepsCursor, succeededSteps);
            stepsCursor = processingSteps.stepsCursor;
            succeededSteps = processingSteps.succeededSteps;
            return [jobState, ...processingSteps.entries];
        }

        const nextJobState = formattedJobStates[index + 1];
        if (isRetried(jobState, nextJobState)) return [jobState, createRetryStep(nextJobState, ++retryCount)];
        if (isRequeued(jobState, nextJobState)) return [jobState, createRequeueStep(nextJobState, ++requeueCount)];

        return entries;
    });
};
