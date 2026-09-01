import {
    buildJobTimeline,
    formatDuration,
    getStepsFromMetadata,
    getTicks,
    STATES,
    STEP_ROW,
    toMicros
} from './job-timeline.js';

const enqueued = (createdAt) => ({"@class": "org.jobrunr.jobs.states.EnqueuedState", state: "ENQUEUED", createdAt});
const processing = (createdAt, updatedAt = createdAt) => ({
    "@class": "org.jobrunr.jobs.states.ProcessingState",
    state: "PROCESSING",
    createdAt,
    updatedAt,
    serverId: "67936c40-7915-41e0-9960-edfff7759aaa",
    serverName: "server-1"
});
const succeeded = (createdAt) => ({state: "SUCCEEDED", createdAt, latencyDuration: 1.0, processDuration: 2.0});
const failed = (createdAt) => ({state: "FAILED", createdAt, message: "Boom", exceptionType: "java.lang.RuntimeException"});
const scheduled = (createdAt, scheduledAt) => ({state: "SCHEDULED", createdAt, scheduledAt, reason: "Retry 1 of 10"});
const deleted = (createdAt) => ({state: "DELETED", createdAt, reason: "Deleted via dashboard"});

// Jackson wraps metadata values with their type, other JsonMappers keep the raw value
const jacksonInstant = (value) => ["java.time.Instant", value];
const at = (isoString) => toMicros(isoString);

describe('toMicros', () => {
    it('keeps the microseconds of an ISO-8601 instant', () => {
        expect(at("2025-09-01T10:00:00.123456Z") - at("2025-09-01T10:00:00Z")).toBe(123456);
    });

    it('supports instants without and with less than 6 fractional digits', () => {
        expect(at("2025-09-01T10:00:00Z") % 1000000).toBe(0);
        expect(at("2025-09-01T10:00:00.5Z") - at("2025-09-01T10:00:00Z")).toBe(500000);
    });

    it('supports epoch seconds and epoch millis', () => {
        expect(toMicros(1756720800)).toBe(at("2025-09-01T10:00:00Z"));
        expect(toMicros(1756720800000)).toBe(at("2025-09-01T10:00:00Z"));
    });
});

describe('getStepsFromMetadata', () => {
    const metadata = {
        "@class": "java.util.concurrent.ConcurrentHashMap",
        "jobRunrDashboardLog-1": {"@class": "org.jobrunr.jobs.context.JobDashboardLogger$JobDashboardLogLines"},
        "jr_step_start_step-1": jacksonInstant("2025-09-01T10:00:01Z"),
        "jr_step_end_step-1": jacksonInstant("2025-09-01T10:00:16Z"),
        "jr_step_step-1": ["java.lang.Boolean", true],
        "jr_step_result_step-1": ["java.lang.String", "result-1"],
        "jr_step_result_class_step-1": ["java.lang.String", "java.lang.String"],
        "jr_step_start_step-2": jacksonInstant("2025-09-01T10:00:16Z"),
        "jr_step_end_step-2": jacksonInstant("2025-09-01T10:00:16.000500Z"),
        "jr_step_step-2": ["java.lang.Boolean", false],
    };

    it('reconstructs the steps of a job', () => {
        expect(getStepsFromMetadata(metadata)).toEqual([
            {
                name: "step-1",
                start: at("2025-09-01T10:00:01Z"),
                end: at("2025-09-01T10:00:16Z"),
                succeeded: true,
                result: "result-1",
                resultClass: "java.lang.String"
            },
            {
                name: "step-2",
                start: at("2025-09-01T10:00:16Z"),
                end: at("2025-09-01T10:00:16.000500Z"),
                succeeded: false,
                result: undefined,
                resultClass: undefined
            },
        ]);
    });

    it('also supports JsonMappers that do not wrap metadata values with their type', () => {
        const steps = getStepsFromMetadata({
            "jr_step_start_step-1": "2025-09-01T10:00:01Z",
            "jr_step_end_step-1": "2025-09-01T10:00:16Z",
            "jr_step_step-1": true,
        });

        expect(steps).toHaveLength(1);
        expect(steps[0].succeeded).toBe(true);
    });

    it('ignores steps that did not report a start time', () => {
        expect(getStepsFromMetadata({"jr_step_step-1": true})).toEqual([]);
        expect(getStepsFromMetadata(undefined)).toEqual([]);
    });
});

describe('buildJobTimeline', () => {
    const now = new Date("2025-09-01T10:01:00Z").getTime();

    it('returns null if the job has no history', () => {
        expect(buildJobTimeline(undefined, now)).toBeNull();
        expect(buildJobTimeline({jobHistory: []}, now)).toBeNull();
    });

    it('gives each state its own row and lets it last until the next state starts', () => {
        const job = {
            jobHistory: [
                enqueued("2025-09-01T10:00:00Z"),
                processing("2025-09-01T10:00:01Z"),
                succeeded("2025-09-01T10:00:31Z"),
            ]
        };

        const timeline = buildJobTimeline(job, now);

        expect(timeline.rows.map(row => row.type)).toEqual([STATES.ENQUEUED, STATES.PROCESSING, STATES.SUCCEEDED]);
        expect(timeline.rows[0].end - timeline.rows[0].start).toBe(1000000);
        expect(timeline.rows[1].end - timeline.rows[1].start).toBe(30000000);
        expect(timeline.rows[2].isMoment).toBe(true);
        expect(timeline.attempts).toBe(1);
        expect(timeline.isRunning).toBe(false);
        expect(timeline.duration).toBe(31000000);
    });

    it('does not consider a job that ended in a terminal state as still running', () => {
        [succeeded("2025-09-01T10:00:31Z"), failed("2025-09-01T10:00:31Z"), deleted("2025-09-01T10:00:31Z")].forEach(lastState => {
            const timeline = buildJobTimeline({
                jobHistory: [enqueued("2025-09-01T10:00:00Z"), processing("2025-09-01T10:00:01Z"), lastState]
            }, now);

            expect(timeline.isRunning).toBe(false);
            expect(timeline.rows.every(row => !row.isRunning)).toBe(true);
        });
    });

    it('lets the state a job is currently in run until now', () => {
        const timeline = buildJobTimeline({
            jobHistory: [enqueued("2025-09-01T10:00:00Z"), processing("2025-09-01T10:00:01Z")]
        }, now);

        expect(timeline.isRunning).toBe(true);
        expect(timeline.rows[1].isRunning).toBe(true);
        expect(timeline.rows[1].end).toBe(now * 1000);
        expect(timeline.end).toBe(now * 1000);
    });

    it('extends the timeline to the moment a scheduled job will run', () => {
        const timeline = buildJobTimeline({
            jobHistory: [
                enqueued("2025-09-01T10:00:00Z"),
                processing("2025-09-01T10:00:01Z"),
                failed("2025-09-01T10:00:31Z"),
                scheduled("2025-09-01T10:00:31Z", "2025-09-01T10:05:00Z"),
            ]
        }, now);

        const scheduledRow = timeline.rows[timeline.rows.length - 1];
        expect(scheduledRow.isRunning).toBe(true);
        expect(scheduledRow.end).toBe(at("2025-09-01T10:05:00Z"));
        expect(timeline.end).toBe(at("2025-09-01T10:05:00Z"));
    });

    it('counts an attempt for every time the job is picked up again', () => {
        const timeline = buildJobTimeline({
            jobHistory: [
                enqueued("2025-09-01T10:00:00Z"),
                processing("2025-09-01T10:00:01Z"),
                failed("2025-09-01T10:00:31Z"),
                scheduled("2025-09-01T10:00:31Z", "2025-09-01T10:00:40Z"),
                enqueued("2025-09-01T10:00:40Z"),
                processing("2025-09-01T10:00:41Z"),
                succeeded("2025-09-01T10:00:50Z"),
            ]
        }, now);

        expect(timeline.attempts).toBe(2);
        expect(timeline.rows.filter(row => row.startsNewAttempt).map(row => row.type)).toEqual([STATES.SCHEDULED]);
        expect(timeline.rows.map(row => row.attempt)).toEqual([1, 1, 1, 2, 2, 2, 2]);
    });

    it('shows the steps of a job below the processing state they ran in', () => {
        const job = {
            jobHistory: [
                enqueued("2025-09-01T10:00:00Z"),
                processing("2025-09-01T10:00:01Z"),
                failed("2025-09-01T10:00:31Z"),
                enqueued("2025-09-01T10:00:40Z"),
                processing("2025-09-01T10:00:41Z"),
                succeeded("2025-09-01T10:00:50Z"),
            ],
            metadata: {
                "jr_step_start_step-1": jacksonInstant("2025-09-01T10:00:02Z"),
                "jr_step_end_step-1": jacksonInstant("2025-09-01T10:00:31Z"),
                "jr_step_step-1": ["java.lang.Boolean", false],
                "jr_step_start_step-2": jacksonInstant("2025-09-01T10:00:42Z"),
                "jr_step_end_step-2": jacksonInstant("2025-09-01T10:00:42.000500Z"),
                "jr_step_step-2": ["java.lang.Boolean", true],
            }
        };

        const timeline = buildJobTimeline(job, now);

        expect(timeline.rows.map(row => row.type)).toEqual([
            STATES.ENQUEUED, STATES.PROCESSING, STEP_ROW, STATES.FAILED,
            STATES.ENQUEUED, STATES.PROCESSING, STEP_ROW, STATES.SUCCEEDED,
        ]);
        expect(timeline.rows[2]).toMatchObject({label: "step-1", succeeded: false, attempt: 1});
        expect(timeline.rows[6]).toMatchObject({label: "step-2", succeeded: true, attempt: 2});
        expect(timeline.rows[6].end - timeline.rows[6].start).toBe(500);
    });

    it('lets a step that did not report an end time run until now if the job is still processing', () => {
        const timeline = buildJobTimeline({
            jobHistory: [enqueued("2025-09-01T10:00:00Z"), processing("2025-09-01T10:00:01Z")],
            metadata: {"jr_step_start_step-1": jacksonInstant("2025-09-01T10:00:02Z")}
        }, now);

        expect(timeline.rows[2]).toMatchObject({label: "step-1", isRunning: true, hasUnknownEnd: false});
        expect(timeline.rows[2].end).toBe(now * 1000);
    });

    it('marks a step without end time of a job that is no longer processing as unknown', () => {
        const timeline = buildJobTimeline({
            jobHistory: [
                enqueued("2025-09-01T10:00:00Z"),
                processing("2025-09-01T10:00:01Z"),
                failed("2025-09-01T10:00:31Z"),
            ],
            metadata: {"jr_step_start_step-1": jacksonInstant("2025-09-01T10:00:02Z")}
        }, now);

        expect(timeline.rows[2]).toMatchObject({label: "step-1", isRunning: false, hasUnknownEnd: true});
        expect(timeline.rows[2].end).toBe(at("2025-09-01T10:00:31Z"));
    });

    it('still shows steps that cannot be matched with a processing state', () => {
        const timeline = buildJobTimeline({
            jobHistory: [scheduled("2025-09-01T10:00:00Z", "2025-09-01T10:05:00Z")],
            metadata: {
                "jr_step_start_step-1": jacksonInstant("2025-08-31T10:00:02Z"),
                "jr_step_end_step-1": jacksonInstant("2025-08-31T10:00:03Z"),
            }
        }, now);

        expect(timeline.rows.map(row => row.type)).toEqual([STATES.SCHEDULED, STEP_ROW]);
        expect(timeline.start).toBe(at("2025-08-31T10:00:02Z"));
    });
});

describe('formatDuration', () => {
    it('formats a duration in the largest unit that keeps it readable', () => {
        expect(formatDuration(1)).toBe("1 µs");
        expect(formatDuration(500)).toBe("500 µs");
        expect(formatDuration(1500)).toBe("1.5 ms");
        expect(formatDuration(845000)).toBe("845 ms");
        expect(formatDuration(1420000)).toBe("1.42 s");
        expect(formatDuration(45000000)).toBe("45 s");
        expect(formatDuration(125000000)).toBe("2 min 5 s");
        expect(formatDuration(120000000)).toBe("2 min");
        expect(formatDuration(3900000000)).toBe("1 h 5 min");
        expect(formatDuration(180000000000)).toBe("2 d 2 h");
    });
});

describe('getTicks', () => {
    it('divides the timeline in round intervals', () => {
        expect(getTicks(31000000).map(tick => tick.offset)).toEqual([0, 10000000, 20000000, 30000000]);
        expect(getTicks(500).map(tick => tick.offset)).toEqual([0, 100, 200, 300, 400, 500]);
    });

    it('always returns a tick for a job without duration', () => {
        expect(getTicks(0)).toEqual([{offset: 0, position: 0}]);
    });
});
