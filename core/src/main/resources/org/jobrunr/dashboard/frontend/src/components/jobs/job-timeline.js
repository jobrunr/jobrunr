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

export const STEP_ROW = 'STEP';

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
 * Reconstructs the steps executed by `JobContext.runStepOnce` from the job metadata. Only steps that
 * report a start time are returned: steps executed via the `ThrowingRunnable` overload do not record
 * timings and can hence not be placed on a timeline.
 */
export const getStepsFromMetadata = (metadata) => {
    if (!metadata) return [];

    const valueOf = (key) => (key in metadata ? unwrapMetadataValue(metadata[key]) : undefined);

    return Object.keys(metadata)
        .filter((key) => key.startsWith(STEP_START_PREFIX))
        .map((key) => {
            const name = key.substring(STEP_START_PREFIX.length);
            const succeeded = valueOf(STEP_PREFIX + name);
            return {
                name,
                start: toMicros(valueOf(key)),
                end: toMicros(valueOf(STEP_END_PREFIX + name)),
                succeeded: typeof succeeded === 'string' ? succeeded === 'true' : succeeded,
                result: valueOf(STEP_RESULT_PREFIX + name),
                resultClass: valueOf(STEP_RESULT_CLASS_PREFIX + name),
            };
        })
        .filter((step) => step.start !== null)
        .sort((left, right) => left.start - right.start);
};

const isMomentInTime = (stateName) => MOMENT_IN_TIME_STATES.includes(stateName);

/** A SCHEDULED or AWAITING job already knows when it will (at the earliest) resume. */
const getPlannedEnd = (jobState) => {
    if (jobState.state === STATES.SCHEDULED) return toMicros(jobState.scheduledAt);
    if (jobState.state === STATES.AWAITING) return toMicros(jobState.to ?? jobState.preferredInstant);
    return null;
};

const toStateRows = (jobHistory, nowInMicros) => {
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
            type: jobState.state,
            label: jobState.state.charAt(0) + jobState.state.slice(1).toLowerCase(),
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

const toStepRow = (step, processingRow, nowInMicros) => {
    const isRunning = step.end === null && (processingRow?.isRunning ?? false);
    const hasUnknownEnd = step.end === null && !isRunning;
    const end = step.end ?? (isRunning ? nowInMicros : processingRow?.end ?? step.start);

    return {
        key: `step-${step.name}`,
        type: STEP_ROW,
        label: step.name,
        start: step.start,
        end: end < step.start ? step.start : end,
        isRunning,
        isMoment: false,
        hasUnknownEnd,
        succeeded: step.succeeded,
        result: step.result,
        attempt: processingRow?.attempt,
        step,
    };
};

/**
 * @param job the job as returned by the `/api/jobs/:id` endpoint
 * @param nowInMillis the current time, used to render the states and steps that are still running
 * @returns {null|{start, end, rows, attempts, isRunning, duration}} null if there is nothing to show
 */
export const buildJobTimeline = (job, nowInMillis = Date.now()) => {
    const jobHistory = job?.jobHistory ?? [];
    if (jobHistory.length === 0) return null;

    const nowInMicros = nowInMillis * MICROS_PER_MILLI;
    const stateRows = toStateRows(jobHistory, nowInMicros);
    const steps = getStepsFromMetadata(job?.metadata);

    // steps are shown right below the PROCESSING state they ran in
    const rows = [];
    const unplacedSteps = [...steps];
    stateRows.forEach((stateRow) => {
        rows.push(stateRow);
        if (stateRow.type !== STATES.PROCESSING) return;
        const stepsOfAttempt = unplacedSteps.filter((step) => step.start >= stateRow.start && step.start <= stateRow.end);
        stepsOfAttempt.forEach((step) => {
            unplacedSteps.splice(unplacedSteps.indexOf(step), 1);
            rows.push(toStepRow(step, stateRow, nowInMicros));
        });
    });
    // steps we could not match with a PROCESSING state (e.g. a job whose history was truncated)
    unplacedSteps.forEach((step) => rows.push(toStepRow(step, undefined, nowInMicros)));

    const start = Math.min(...rows.map((row) => row.start));
    const end = Math.max(...rows.map((row) => row.end));
    const lastState = stateRows[stateRows.length - 1];

    return {
        start,
        end: end > start ? end : start + 1,
        duration: end - start,
        rows,
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
