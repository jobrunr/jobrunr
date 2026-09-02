/**
 * Builds the view model behind the job execution timeline (a Gantt like visualization of everything
 * that happened to a job).
 *
 * A job is a sequence of {@link org.jobrunr.jobs.states.JobState}s (its jobHistory): a job can be
 * AWAITING, SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED, FAILED or DELETED and it can go through these
 * states multiple times (e.g. PROCESSING -> FAILED -> SCHEDULED -> ENQUEUED -> PROCESSING on a retry).
 * On top of that, a job can report the steps it executed via {@code JobContext.runStepOnce} - these are
 * stored as job metadata and are visualized inside the PROCESSING state they ran in.
 */

export const STATES = {
    AWAITING: 'AWAITING',
    SCHEDULED: 'SCHEDULED',
    ENQUEUED: 'ENQUEUED',
    PROCESSING: 'PROCESSING',
    SUCCEEDED: 'SUCCEEDED',
    FAILED: 'FAILED',
    DELETED: 'DELETED',
};

export const STEP = 'STEP';

/** States that mark a moment in time instead of a period: they end the attempt they belong to. */
const MOMENT_IN_TIME_STATES = [STATES.SUCCEEDED, STATES.FAILED, STATES.DELETED];

const STEP_PREFIX = 'jr_step_';
const STEP_START_PREFIX = 'jr_step_start_';
const STEP_END_PREFIX = 'jr_step_end_';
const STEP_RESULT_PREFIX = 'jr_step_result_';
const STEP_RESULT_CLASS_PREFIX = 'jr_step_result_class_';

const MICROS_PER_MILLI = 1000;
const MICROS_PER_SECOND = 1000 * MICROS_PER_MILLI;
const MICROS_PER_MINUTE = 60 * MICROS_PER_SECOND;
const MICROS_PER_HOUR = 60 * MICROS_PER_MINUTE;
const MICROS_PER_DAY = 24 * MICROS_PER_HOUR;

const ISO_INSTANT_PATTERN = /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(?::\d{2})?)(?:\.(\d+))?(Z|[+-]\d{2}:?\d{2})?$/;

/**
 * Instants are serialized as ISO-8601 Strings but - depending on the configured JsonMapper - can also
 * arrive as epoch seconds or millis. Everything is converted to epoch microseconds as job steps can be
 * shorter than a millisecond and `Date` would round these away.
 */
export const toMicros = (value) => {
    if (value === null || value === undefined) return null;
    if (value instanceof Date) return value.getTime() * MICROS_PER_MILLI;
    if (typeof value === 'number') {
        // epoch seconds (with optional fraction) for the smaller numbers, epoch millis for the larger ones
        return Math.round(value < 1e11 ? value * MICROS_PER_SECOND : value * MICROS_PER_MILLI);
    }

    const matcher = ISO_INSTANT_PATTERN.exec(String(value));
    if (!matcher) {
        const parsed = Date.parse(String(value));
        return Number.isNaN(parsed) ? null : parsed * MICROS_PER_MILLI;
    }
    const [, dateTime, fraction, zone] = matcher;
    const millisOfWholeSeconds = Date.parse(dateTime + (zone ?? 'Z'));
    if (Number.isNaN(millisOfWholeSeconds)) return null;
    const micros = fraction ? parseInt((fraction + '000000').slice(0, 6), 10) : 0;
    return millisOfWholeSeconds * MICROS_PER_MILLI + micros;
};

/**
 * Metadata values are wrapped by the JsonMapper with their type: Jackson serializes them as
 * `["java.time.Instant", "2025-09-01T10:00:00Z"]` while Gson and JSON-B keep the raw value.
 */
export const unwrapMetadataValue = (value) => {
    if (Array.isArray(value) && value.length === 2 && typeof value[0] === 'string') return value[1];
    return value;
};

/**
 * `JobContext.runStepOnce` keeps every run of a step by appending `__<amount of job states>` to the
 * metadata key, so a step that ran again after a retry is stored next to its earlier run. That suffix is
 * an implementation detail of the storage: it identifies a run but is never shown to the user.
 */
const STEP_RUN_SUFFIX_PATTERN = /^(.+)__(\d+)$/;

const toStepName = (metadataName) => STEP_RUN_SUFFIX_PATTERN.exec(metadataName)?.[1] ?? metadataName;

/**
 * Reconstructs the runs of the steps executed by `JobContext.runStepOnce` from the job metadata. Only
 * runs that report a start time are returned: steps executed via the `ThrowingRunnable` overload do not
 * record timings and can hence not be placed on a timeline.
 */
export const getStepsFromMetadata = (metadata) => {
    if (!metadata) return [];

    const valueOf = (key) => (key in metadata ? unwrapMetadataValue(metadata[key]) : undefined);

    return Object.keys(metadata)
        .filter((key) => key.startsWith(STEP_START_PREFIX))
        .map((key) => {
            // the metadata name of a run, e.g. `send-email__6`, of which only `send-email` is shown
            const metadataName = key.substring(STEP_START_PREFIX.length);
            const succeeded = valueOf(STEP_PREFIX + metadataName);
            return {
                name: toStepName(metadataName),
                metadataName,
                start: toMicros(valueOf(key)),
                end: toMicros(valueOf(STEP_END_PREFIX + metadataName)),
                succeeded: typeof succeeded === 'string' ? succeeded === 'true' : succeeded,
                result: valueOf(STEP_RESULT_PREFIX + metadataName),
                resultClass: valueOf(STEP_RESULT_CLASS_PREFIX + metadataName),
            };
        })
        .filter((step) => step.start !== null)
        .sort((left, right) => left.start - right.start);
};

const isMomentInTime = (stateName) => MOMENT_IN_TIME_STATES.includes(stateName);

const toLabel = (stateName) => stateName.charAt(0) + stateName.slice(1).toLowerCase();

/** A SCHEDULED or AWAITING job already knows when it will (at the earliest) resume. */
const getPlannedEnd = (jobState) => {
    if (jobState.state === STATES.SCHEDULED) return toMicros(jobState.scheduledAt);
    if (jobState.state === STATES.AWAITING) return toMicros(jobState.to ?? jobState.preferredInstant);
    return null;
};

const toStateSegments = (jobHistory, nowInMicros) => {
    let attempt = 1;
    return jobHistory.map((jobState, index) => {
        const previous = jobHistory[index - 1];
        const next = jobHistory[index + 1];
        const startsNewAttempt = previous !== undefined && isMomentInTime(previous.state);
        if (startsNewAttempt) attempt++;

        const start = toMicros(jobState.createdAt);
        let end;
        let isRunning = false;
        let plannedEnd = null;
        if (isMomentInTime(jobState.state)) {
            end = start;
        } else if (next) {
            end = toMicros(next.createdAt);
        } else {
            isRunning = true;
            plannedEnd = getPlannedEnd(jobState);
            end = Math.max(nowInMicros, plannedEnd ?? nowInMicros);
        }

        return {
            key: `state-${index}`,
            kind: 'state',
            type: jobState.state,
            label: toLabel(jobState.state),
            start,
            end: end === null || end < start ? start : end,
            isRunning,
            isMoment: isMomentInTime(jobState.state),
            plannedEnd,
            attempt,
            startsNewAttempt,
            jobState,
        };
    });
};

const toStepSegment = (run, processingSegment, nowInMicros) => {
    const isRunning = run.end === null && (processingSegment?.isRunning ?? false);
    const hasUnknownEnd = run.end === null && !isRunning;
    const end = run.end ?? (isRunning ? nowInMicros : processingSegment?.end ?? run.start);

    return {
        key: `step-${run.metadataName}`,
        kind: 'step',
        type: STEP,
        label: run.name,
        start: run.start,
        end: end < run.start ? run.start : end,
        isRunning,
        isMoment: false,
        hasUnknownEnd,
        succeeded: run.succeeded,
        result: run.result,
        attempt: processingSegment?.attempt,
        run,
    };
};

/**
 * A step that already completed successfully is not executed again when the job is retried (see
 * {@code JobContext.hasCompletedStep}). Nothing is stored for something that did not run, so a skip is
 * derived: the step completed during an earlier attempt and has no run of its own in this one.
 */
const toSkippedSegment = (stepName, completedDuringAttempt, processingSegment) => ({
    key: `skipped-${stepName}-${processingSegment.key}`,
    kind: 'skipped',
    type: STEP,
    label: stepName,
    start: processingSegment.start,
    end: processingSegment.start,
    attempt: processingSegment.attempt,
    completedDuringAttempt,
});

const toMarker = (skippedSegment) => ({
    key: skippedSegment.key,
    at: skippedSegment.start,
    stepName: skippedSegment.label,
    attempt: skippedSegment.attempt,
    completedDuringAttempt: skippedSegment.completedDuringAttempt,
});

const durationOfSpans = (spans) => spans
    .filter((span) => !span.isMoment)
    .reduce((total, span) => total + (span.end - span.start), 0);

const toLane = (lane) => ({
    ...lane,
    count: lane.spans.length,
    duration: durationOfSpans(lane.spans),
    didNotRun: lane.spans.length === 0,
});

/** The detailed view gives every state, every run of a step and every skipped step a lane of its own. */
const toDetailedLanes = (segments) => segments.map((segment) => toLane({
    key: segment.key,
    label: segment.label,
    type: segment.type,
    isStepLane: segment.kind !== 'state',
    attempt: segment.attempt,
    startsNewAttempt: segment.startsNewAttempt,
    spans: segment.kind === 'skipped' ? [] : [segment],
    markers: segment.kind === 'skipped' ? [toMarker(segment)] : [],
}));

const COMPACT_LANE_ORDER = [STATES.AWAITING, STATES.SCHEDULED, STATES.ENQUEUED, STATES.PROCESSING];

/**
 * The compact view has one lane per kind of activity: the second time a job is processed is drawn as an
 * extra span on the same PROCESSING lane and the states that mark a moment in time (SUCCEEDED, FAILED,
 * DELETED) are drawn at the end of the span of the state they ended. A step keeps a lane of its own, on
 * which every run of that step is drawn.
 */
const toCompactLanes = (segments) => {
    const spansPerLane = new Map();
    const addTo = (laneType, segment) => {
        if (!spansPerLane.has(laneType)) spansPerLane.set(laneType, []);
        spansPerLane.get(laneType).push(segment);
    };

    let previousStateType = null;
    segments.filter((segment) => segment.kind === 'state').forEach((segment) => {
        if (segment.isMoment) {
            addTo(previousStateType ?? segment.type, segment);
        } else {
            previousStateType = segment.type;
            addTo(segment.type, segment);
        }
    });

    const stateLanes = COMPACT_LANE_ORDER
        .filter((laneType) => spansPerLane.has(laneType))
        .map((laneType) => toLane({
            key: `lane-${laneType}`,
            label: toLabel(laneType),
            type: laneType,
            isStepLane: false,
            spans: spansPerLane.get(laneType),
            markers: [],
        }));

    return [...stateLanes, ...toCompactStepLanes(segments)];
};

/** All runs of the same step - and the attempts it was skipped in - share a lane. */
const toCompactStepLanes = (segments) => {
    const stepNames = [...new Set(segments.filter((segment) => segment.kind !== 'state').map((segment) => segment.label))];
    return stepNames.map((stepName) => toLane({
        key: `lane-step-${stepName}`,
        label: stepName,
        type: STEP,
        isStepLane: true,
        spans: segments.filter((segment) => segment.kind === 'step' && segment.label === stepName),
        markers: segments.filter((segment) => segment.kind === 'skipped' && segment.label === stepName).map(toMarker),
    }));
};

/**
 * Places the runs of the steps below the PROCESSING state they ran in, preceded by the steps that were
 * skipped during that attempt because they already completed earlier.
 */
const toSegments = (stateSegments, stepRuns, nowInMicros) => {
    const segments = [];
    const unplacedRuns = [...stepRuns];
    const attemptStepSucceededIn = new Map();

    stateSegments.forEach((stateSegment) => {
        segments.push(stateSegment);
        if (stateSegment.type !== STATES.PROCESSING) return;

        const runs = unplacedRuns.filter((run) => run.start >= stateSegment.start && run.start <= stateSegment.end);
        runs.forEach((run) => unplacedRuns.splice(unplacedRuns.indexOf(run), 1));

        const namesThatRan = new Set(runs.map((run) => run.name));
        attemptStepSucceededIn.forEach((attempt, stepName) => {
            if (!namesThatRan.has(stepName)) segments.push(toSkippedSegment(stepName, attempt, stateSegment));
        });

        runs.forEach((run) => segments.push(toStepSegment(run, stateSegment, nowInMicros)));
        runs.filter((run) => run.succeeded === true)
            .forEach((run) => attemptStepSucceededIn.set(run.name, stateSegment.attempt));
    });

    // runs we could not match with a PROCESSING state (e.g. a job whose history was truncated)
    unplacedRuns.forEach((run) => segments.push(toStepSegment(run, undefined, nowInMicros)));
    return segments;
};

/**
 * @param job the job as returned by the `/api/jobs/:id` endpoint
 * @param nowInMillis the current time, used to render the states and steps that are still running
 * @returns {null|{start, end, duration, segments, detailedLanes, compactLanes, attempts, isRunning}}
 */
export const buildJobTimeline = (job, nowInMillis = Date.now()) => {
    const jobHistory = job?.jobHistory ?? [];
    if (jobHistory.length === 0) return null;

    const nowInMicros = nowInMillis * MICROS_PER_MILLI;
    const stateSegments = toStateSegments(jobHistory, nowInMicros);
    const segments = toSegments(stateSegments, getStepsFromMetadata(job?.metadata), nowInMicros);

    const start = Math.min(...segments.map((segment) => segment.start));
    const end = Math.max(...segments.map((segment) => segment.end));
    const lastState = stateSegments[stateSegments.length - 1];

    return {
        start,
        end: end > start ? end : start + 1,
        duration: end - start,
        segments,
        detailedLanes: toDetailedLanes(segments),
        compactLanes: toCompactLanes(segments),
        attempts: lastState.attempt,
        isRunning: lastState.isRunning,
        now: nowInMicros,
    };
};

const round = (value, decimals) => {
    const rounded = value.toFixed(decimals);
    return decimals > 0 ? rounded.replace(/\.?0+$/, '') : rounded;
};

export const formatDuration = (micros) => {
    if (micros === null || micros === undefined) return '';
    if (micros < MICROS_PER_MILLI) return `${Math.round(micros)} µs`;
    if (micros < MICROS_PER_SECOND) return `${round(micros / MICROS_PER_MILLI, micros < 10 * MICROS_PER_MILLI ? 2 : 0)} ms`;
    if (micros < MICROS_PER_MINUTE) return `${round(micros / MICROS_PER_SECOND, micros < 10 * MICROS_PER_SECOND ? 2 : 1)} s`;
    if (micros < MICROS_PER_HOUR) {
        const minutes = Math.floor(micros / MICROS_PER_MINUTE);
        const seconds = Math.round((micros % MICROS_PER_MINUTE) / MICROS_PER_SECOND);
        return seconds > 0 ? `${minutes} min ${seconds} s` : `${minutes} min`;
    }
    if (micros < MICROS_PER_DAY) {
        const hours = Math.floor(micros / MICROS_PER_HOUR);
        const minutes = Math.round((micros % MICROS_PER_HOUR) / MICROS_PER_MINUTE);
        return minutes > 0 ? `${hours} h ${minutes} min` : `${hours} h`;
    }
    const days = Math.floor(micros / MICROS_PER_DAY);
    const hours = Math.round((micros % MICROS_PER_DAY) / MICROS_PER_HOUR);
    return hours > 0 ? `${days} d ${hours} h` : `${days} d`;
};

/** Formats an offset relative to the start of the timeline, e.g. `+1.5 s`. */
export const formatOffset = (micros) => (micros <= 0 ? '0' : `+${formatDuration(micros)}`);

const pad = (value, length = 2) => String(value).padStart(length, '0');

export const formatTime = (micros) => {
    const date = new Date(Math.floor(micros / MICROS_PER_MILLI));
    const subSecond = pad(date.getMilliseconds(), 3) + pad(Math.floor(micros % MICROS_PER_MILLI), 3);
    return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}.${subSecond}`;
};

export const formatDateTime = (micros) => {
    const date = new Date(Math.floor(micros / MICROS_PER_MILLI));
    return `${date.toLocaleDateString()} ${formatTime(micros)}`;
};

const TICK_STEPS = [
    1, 2, 5, 10, 25, 50, 100, 250, 500,
    MICROS_PER_MILLI, 2 * MICROS_PER_MILLI, 5 * MICROS_PER_MILLI, 10 * MICROS_PER_MILLI, 25 * MICROS_PER_MILLI,
    50 * MICROS_PER_MILLI, 100 * MICROS_PER_MILLI, 250 * MICROS_PER_MILLI, 500 * MICROS_PER_MILLI,
    MICROS_PER_SECOND, 2 * MICROS_PER_SECOND, 5 * MICROS_PER_SECOND, 10 * MICROS_PER_SECOND, 15 * MICROS_PER_SECOND, 30 * MICROS_PER_SECOND,
    MICROS_PER_MINUTE, 2 * MICROS_PER_MINUTE, 5 * MICROS_PER_MINUTE, 10 * MICROS_PER_MINUTE, 15 * MICROS_PER_MINUTE, 30 * MICROS_PER_MINUTE,
    MICROS_PER_HOUR, 2 * MICROS_PER_HOUR, 3 * MICROS_PER_HOUR, 6 * MICROS_PER_HOUR, 12 * MICROS_PER_HOUR,
    MICROS_PER_DAY, 2 * MICROS_PER_DAY, 7 * MICROS_PER_DAY, 30 * MICROS_PER_DAY,
];

/**
 * Divides the timeline in at most `maxTicks` human friendly intervals (1, 2 or 5 based) so that the
 * gridlines land on round durations instead of on arbitrary fractions of the total duration.
 */
export const getTicks = (duration, maxTicks = 5) => {
    if (!(duration > 0)) return [{offset: 0, position: 0}];
    const step = TICK_STEPS.find((candidate) => duration / candidate <= maxTicks) ?? duration / maxTicks;
    const ticks = [];
    for (let offset = 0; offset <= duration; offset += step) {
        ticks.push({offset, position: (offset / duration) * 100});
    }
    return ticks;
};
