import {
    buildJobTimeline,
    formatDuration,
    getStepsFromMetadata,
    getTicks,
    STATES,
    STEP,
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

        expect(timeline.segments.map(row => row.type)).toEqual([STATES.ENQUEUED, STATES.PROCESSING, STATES.SUCCEEDED]);
        expect(timeline.segments[0].end - timeline.segments[0].start).toBe(1000000);
        expect(timeline.segments[1].end - timeline.segments[1].start).toBe(30000000);
        expect(timeline.segments[2].isMoment).toBe(true);
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
            expect(timeline.segments.every(row => !row.isRunning)).toBe(true);
        });
    });

    it('lets the state a job is currently in run until now', () => {
        const timeline = buildJobTimeline({
            jobHistory: [enqueued("2025-09-01T10:00:00Z"), processing("2025-09-01T10:00:01Z")]
        }, now);

        expect(timeline.isRunning).toBe(true);
        expect(timeline.segments[1].isRunning).toBe(true);
        expect(timeline.segments[1].end).toBe(now * 1000);
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

        const scheduledRow = timeline.segments[timeline.segments.length - 1];
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
        expect(timeline.segments.filter(row => row.startsNewAttempt).map(row => row.type)).toEqual([STATES.SCHEDULED]);
        expect(timeline.segments.map(row => row.attempt)).toEqual([1, 1, 1, 2, 2, 2, 2]);
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

        expect(timeline.segments.map(row => row.type)).toEqual([
            STATES.ENQUEUED, STATES.PROCESSING, STEP, STATES.FAILED,
            STATES.ENQUEUED, STATES.PROCESSING, STEP, STATES.SUCCEEDED,
        ]);
        expect(timeline.segments[2]).toMatchObject({label: "step-1", succeeded: false, attempt: 1});
        expect(timeline.segments[6]).toMatchObject({label: "step-2", succeeded: true, attempt: 2});
        expect(timeline.segments[6].end - timeline.segments[6].start).toBe(500);
    });

    it('lets a step that did not report an end time run until now if the job is still processing', () => {
        const timeline = buildJobTimeline({
            jobHistory: [enqueued("2025-09-01T10:00:00Z"), processing("2025-09-01T10:00:01Z")],
            metadata: {"jr_step_start_step-1": jacksonInstant("2025-09-01T10:00:02Z")}
        }, now);

        expect(timeline.segments[2]).toMatchObject({label: "step-1", isRunning: true, hasUnknownEnd: false});
        expect(timeline.segments[2].end).toBe(now * 1000);
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

        expect(timeline.segments[2]).toMatchObject({label: "step-1", isRunning: false, hasUnknownEnd: true});
        expect(timeline.segments[2].end).toBe(at("2025-09-01T10:00:31Z"));
    });

    it('still shows steps that cannot be matched with a processing state', () => {
        const timeline = buildJobTimeline({
            jobHistory: [scheduled("2025-09-01T10:00:00Z", "2025-09-01T10:05:00Z")],
            metadata: {
                "jr_step_start_step-1": jacksonInstant("2025-08-31T10:00:02Z"),
                "jr_step_end_step-1": jacksonInstant("2025-08-31T10:00:03Z"),
            }
        }, now);

        expect(timeline.segments.map(row => row.type)).toEqual([STATES.SCHEDULED, STEP]);
        expect(timeline.start).toBe(at("2025-08-31T10:00:02Z"));
    });
});

describe('the compact view of buildJobTimeline', () => {
    const now = new Date("2025-09-01T10:01:00Z").getTime();
    const jobRetriedTwice = {
        jobHistory: [
            enqueued("2025-09-01T10:00:00Z"),
            processing("2025-09-01T10:00:01Z"),
            failed("2025-09-01T10:00:31Z"),
            scheduled("2025-09-01T10:00:31Z", "2025-09-01T10:00:40Z"),
            enqueued("2025-09-01T10:00:40Z"),
            processing("2025-09-01T10:00:41Z"),
            succeeded("2025-09-01T10:00:50Z"),
        ],
        metadata: {
            "jr_step_start_step-1": jacksonInstant("2025-09-01T10:00:02Z"),
            "jr_step_end_step-1": jacksonInstant("2025-09-01T10:00:05Z"),
            "jr_step_step-1": ["java.lang.Boolean", true],
            "jr_step_start_step-2": jacksonInstant("2025-09-01T10:00:42Z"),
            "jr_step_end_step-2": jacksonInstant("2025-09-01T10:00:48Z"),
            "jr_step_step-2": ["java.lang.Boolean", true],
        }
    };

    it('draws every attempt as an extra span on the same lane but keeps a lane per step', () => {
        const {compactLanes} = buildJobTimeline(jobRetriedTwice, now);

        expect(compactLanes.map(lane => [lane.label, lane.count])).toEqual([
            ["Scheduled", 1], ["Enqueued", 2], ["Processing", 4], ["step-1", 1], ["step-2", 1]
        ]);
    });

    it('ends the span of a processing lane with the state that terminated it', () => {
        const {compactLanes} = buildJobTimeline(jobRetriedTwice, now);

        const processing = compactLanes.find(lane => lane.label === "Processing");
        expect(processing.spans.map(span => span.type)).toEqual([STATES.PROCESSING, STATES.FAILED, STATES.PROCESSING, STATES.SUCCEEDED]);
        expect(processing.spans[1].start).toBe(processing.spans[0].end);
    });

    it('attaches a deletion to the lane of the state the job was in', () => {
        const {compactLanes} = buildJobTimeline({
            jobHistory: [scheduled("2025-09-01T10:00:00Z", "2025-09-01T11:00:00Z"), deleted("2025-09-01T10:00:10Z")]
        }, now);

        expect(compactLanes.map(lane => lane.label)).toEqual(["Scheduled"]);
        expect(compactLanes[0].spans.map(span => span.type)).toEqual([STATES.SCHEDULED, STATES.DELETED]);
    });

    it('sums the duration of all spans of a lane', () => {
        const {compactLanes} = buildJobTimeline(jobRetriedTwice, now);

        // 1 second before the first attempt and 1 second before the second one
        expect(compactLanes.find(lane => lane.label === "Enqueued").duration).toBe(2000000);
        // 30 seconds for the first attempt and 9 for the second one
        expect(compactLanes.find(lane => lane.label === "Processing").duration).toBe(39000000);
    });

    it('marks the steps that were skipped because they already completed during an earlier attempt', () => {
        const {compactLanes, detailedLanes} = buildJobTimeline(jobRetriedTwice, now);

        [compactLanes, detailedLanes].forEach(lanes => {
            expect(lanes.find(lane => lane.label === "step-1").markers).toEqual([expect.objectContaining({
                at: at("2025-09-01T10:00:41Z"),
                attempt: 2,
                completedDuringAttempt: 1,
                stepNames: ["step-1"],
            })]);
            // step-2 ran during the last attempt, it was never skipped
            expect(lanes.find(lane => lane.label === "step-2").markers).toEqual([]);
        });
    });

    it('does not mark a step that failed as skipped', () => {
        const {compactLanes} = buildJobTimeline({
            ...jobRetriedTwice,
            metadata: {
                "jr_step_start_step-1": jacksonInstant("2025-09-01T10:00:02Z"),
                "jr_step_end_step-1": jacksonInstant("2025-09-01T10:00:05Z"),
                "jr_step_step-1": ["java.lang.Boolean", false],
            }
        }, now);

        expect(compactLanes.find(lane => lane.label === "step-1").markers).toEqual([]);
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
