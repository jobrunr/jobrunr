import {
    addSkippedStepsToPerformedSteps,
    buildTimelineModel,
    convertStepsToTimeline,
    createTimeCompressor,
    generateTimeTicks,
    getStepEndTime,
    getStepLabel,
    groupCompactStepsSequentially,
    removeInitialScheduled,
    toTimelineSteps,
} from './timeline-data.js';

const BASE = Date.UTC(2024, 0, 1, 0, 0, 0);
const iso = (offsetMs) => new Date(BASE + offsetMs).toISOString();
const ms = (offsetMs) => BASE + offsetMs;

describe('getStepEndTime', () => {
    it('returns the updatedAt time in ms when it is after createdAt', () => {
        expect(getStepEndTime({createdAt: iso(0), updatedAt: iso(5000)})).toBe(BASE + 5000);
    });

    it('returns null when updatedAt is missing or not after createdAt', () => {
        expect(getStepEndTime({createdAt: iso(0)})).toBeNull();
        expect(getStepEndTime({createdAt: iso(5000), updatedAt: iso(0)})).toBeNull();
    });
});

describe('removeInitialScheduled', () => {
    it('removes a single leading SCHEDULED step and leaves the rest untouched', () => {
        const steps = [{state: 'SCHEDULED'}, {state: 'ENQUEUED'}, {state: 'SCHEDULED'}];
        expect(removeInitialScheduled(steps)).toEqual([{state: 'ENQUEUED'}, {state: 'SCHEDULED'}]);
    });

    it('does nothing when the first step is not SCHEDULED', () => {
        const steps = [{state: 'ENQUEUED'}, {state: 'SCHEDULED'}];
        expect(removeInitialScheduled(steps)).toBe(steps);
    });

    it('handles empty and nullish input', () => {
        expect(removeInitialScheduled([])).toEqual([]);
        expect(removeInitialScheduled(null)).toEqual([]);
    });
});

describe('getStepLabel', () => {
    it('maps known states to human-readable labels', () => {
        expect(getStepLabel({state: 'ENQUEUED'})).toBe('Enqueued');
        expect(getStepLabel({state: 'PROCESSING'})).toBe('Processing');
    });

    it('uses the base step name for RUN_STEP_ONCE', () => {
        expect(getStepLabel({state: 'RUN_STEP_ONCE', stepName: 'doWork__2'})).toBe('doWork');
    });

    it('falls back to the raw state, then Unknown', () => {
        expect(getStepLabel({state: 'WEIRD'})).toBe('WEIRD');
        expect(getStepLabel({})).toBe('Unknown');
    });

    it('reports consolidated steps as Execution time', () => {
        expect(getStepLabel({isConsolidated: true, state: 'SUCCEEDED'})).toBe('Execution time');
    });
});

describe('toTimelineSteps', () => {
    it('only filters AWAITING and DELETED', () => {
        const steps = [
            {state: 'AWAITING', createdAt: iso(0)},
            {state: 'SCHEDULED', createdAt: iso(1)},
            {state: 'ENQUEUED', createdAt: iso(2)},
            {state: 'DELETED', createdAt: iso(3)},
        ];
        const result = toTimelineSteps(steps);
        expect(result.map((s) => s.state)).toEqual(['SCHEDULED', 'ENQUEUED']);
    });
});

describe('convertStepsToTimeline', () => {
    it('computes start/end and per-step end times for a completed job', () => {
        const steps = [
            {state: 'ENQUEUED', createdAt: iso(0)},
            {state: 'PROCESSING', createdAt: iso(100)},
            {state: 'SUCCEEDED', createdAt: iso(10000)},
        ];
        const {start, end, stepEndTimesMap, skipped} = convertStepsToTimeline(steps, ms(10000));
        expect(start).toBe(ms(0));
        expect(end).toBe(ms(10000));
        expect(skipped).toEqual([]);
        expect(stepEndTimesMap.get(steps[0]).end).toBe(ms(100));
        expect(stepEndTimesMap.get(steps[2]).end).toBe(ms(10000));
        expect(stepEndTimesMap.get(steps[2]).active).toBe(false);
    });

    it('marks the last step active and ending at now for an in-progress job', () => {
        const steps = [{state: 'PROCESSING', createdAt: iso(0)}];
        const {start, end, stepEndTimesMap} = convertStepsToTimeline(steps, ms(10000));
        expect(start).toBe(ms(0));
        expect(end).toBe(ms(10000));
        expect(stepEndTimesMap.get(steps[0]).active).toBe(true);
        expect(stepEndTimesMap.get(steps[0]).end).toBe(ms(10000));
    });

    it('detects skipped RUN_STEP_ONCE steps that succeeded in a prior attempt', () => {
        const steps = [
            {state: 'ENQUEUED', createdAt: iso(0)},
            {state: 'PROCESSING', createdAt: iso(100)},
            {state: 'RUN_STEP_ONCE', stepName: 'stepA__1', succeeded: true, createdAt: iso(200), updatedAt: iso(300)},
            {state: 'RUN_STEP_ONCE', stepName: 'stepB__1', succeeded: false, createdAt: iso(400), updatedAt: iso(500)},
            {state: 'FAILED', createdAt: iso(600)},
            {state: 'SCHEDULED', reason: 'Retry', createdAt: iso(700)},
            {state: 'ENQUEUED', createdAt: iso(800)},
            {state: 'PROCESSING', createdAt: iso(900)},
            {state: 'RUN_STEP_ONCE', stepName: 'stepB__2', succeeded: true, createdAt: iso(1000), updatedAt: iso(1100)},
            {state: 'SUCCEEDED', createdAt: iso(1200)},
        ];
        const {skipped} = convertStepsToTimeline(steps, ms(1200));
        expect(skipped).toHaveLength(1);
        expect(skipped[0]).toMatchObject({stepName: 'stepA__2', attemptId: 2, isSkipped: true, succeeded: true});
        expect(skipped[0].createdAt).toBe(iso(1000));
    });
});

describe('addSkippedStepsToPerformedSteps', () => {
    it('splices each attempt\'s skipped steps ahead of its first real step', () => {
        const steps = [
            {state: 'RUN_STEP_ONCE', stepName: 'stepB__2', succeeded: true, createdAt: iso(1000)},
            {state: 'SUCCEEDED', createdAt: iso(1200)},
        ];
        const skipped = [{state: 'RUN_STEP_ONCE', stepName: 'stepA__2', attemptId: 2, isSkipped: true, createdAt: iso(1000)}];
        const merged = addSkippedStepsToPerformedSteps(steps, skipped);
        const stepBIndex = merged.findIndex((s) => s.stepName === 'stepB__2');
        expect(stepBIndex).toBeGreaterThan(0);
        expect(merged[stepBIndex - 1]).toBe(skipped[0]);
    });
});

describe('createTimeCompressor', () => {
    it('is the identity function when there are no long ranges', () => {
        const compressTime = createTimeCompressor([], 10000, 60000);
        expect(compressTime(0)).toBe(0);
        expect(compressTime(5000)).toBe(5000);
        expect(compressTime(10000)).toBe(10000);
    });

    it('leaves time before the range untouched and compresses time inside it', () => {
        const compressTime = createTimeCompressor([{startMs: 1000, endMs: 5000}], 10000, 60000);
        expect(compressTime(0)).toBe(0);
        expect(compressTime(1000)).toBe(1000);
        expect(compressTime(5000)).toBeLessThan(5000);
        expect(compressTime(5000)).toBeGreaterThan(1000);
    });

    it('is monotonically non-decreasing and linear after the range', () => {
        const compressTime = createTimeCompressor([{startMs: 1000, endMs: 5000}], 10000, 60000);
        const values = [0, 1000, 3000, 5000, 6000].map(compressTime);
        for (let i = 1; i < values.length; i++) expect(values[i]).toBeGreaterThanOrEqual(values[i - 1]);
        expect(compressTime(6000) - compressTime(5000)).toBe(1000);
    });
});

describe('generateTimeTicks', () => {
    it('produces evenly spaced ticks with percentage positions', () => {
        const ticks = generateTimeTicks(10000, (t) => t, 0, 10000, []);
        expect(ticks[0].label).toBe('0');
        expect(ticks.map((t) => t.pct)).toEqual([0, 50, 100]);
        expect(ticks.every((t) => !t.isBreak)).toBe(true);
    });

    it('emits a break tick spanning a long range', () => {
        const ticks = generateTimeTicks(10000, (t) => t, 0, 10000, [{startMs: 2000, endMs: 8000}]);
        const breakTick = ticks.find((t) => t.isBreak);
        expect(breakTick).toBeDefined();
        expect(breakTick.label).toBe('+2s ... +8s');
        expect(breakTick.startMs).toBe(2000);
        expect(breakTick.endMs).toBe(8000);
    });
});

describe('groupCompactStepsSequentially', () => {
    it('groups lifecycle states into rows and records PROCESSING outcomes', () => {
        const steps = [
            {state: 'ENQUEUED', createdAt: iso(0)},
            {state: 'PROCESSING', createdAt: iso(100)},
            {state: 'SUCCEEDED', createdAt: iso(10000)},
        ];
        const {stepEndTimesMap} = convertStepsToTimeline(steps, ms(10000));
        const rows = groupCompactStepsSequentially(steps, stepEndTimesMap, ms(10000), []);
        expect(rows.map((r) => r.key)).toEqual(['ENQUEUED', 'PROCESSING']);
        const processing = rows.find((r) => r.key === 'PROCESSING');
        expect(processing.items[0].outcome).toBe('SUCCEEDED');
        expect(processing.totalMs).toBe(ms(10000) - ms(100));
    });
});

describe('buildTimelineModel', () => {
    const completedJob = () => [
        {state: 'ENQUEUED', createdAt: iso(0)},
        {state: 'PROCESSING', createdAt: iso(100)},
        {state: 'SUCCEEDED', createdAt: iso(10000)},
    ];

    it('returns a populated model with ticks and rows carrying placement data', () => {
        const model = buildTimelineModel({steps: completedJob(), mode: 'compact', compression: 'compressed', reverse: false, now: ms(10000)});
        expect(model.start).toBe(ms(0));
        expect(model.end).toBe(ms(10000));
        expect(model.ticks.length).toBeGreaterThan(0);
        expect(model.compactRows.map((r) => r.key)).toEqual(['ENQUEUED', 'PROCESSING']);
        const item = model.compactRows[0].items[0];
        expect(item.placement).toBeDefined();
        expect(item.placement.offset).toBeGreaterThanOrEqual(0);
        expect(model.orderedDetailedRows[0].item.state).toBe('ENQUEUED');
    });

    it('returns null when there are no displayable steps', () => {
        const model = buildTimelineModel({
            steps: [{state: 'AWAITING', createdAt: iso(0)}],
            mode: 'compact',
            compression: 'compressed',
            reverse: false,
            now: ms(0)
        });
        expect(model).toBeNull();
    });

    it('reverses row order when reverse is true', () => {
        const model = buildTimelineModel({steps: completedJob(), mode: 'compact', compression: 'compressed', reverse: true, now: ms(10000)});
        expect(model.compactRows.map((r) => r.key)).toEqual(['PROCESSING', 'ENQUEUED']);
        expect(model.orderedDetailedRows[0].item.state).toBe('SUCCEEDED');
    });

    it('marks a bar as compressed only when compression is enabled and a long range exists', () => {
        const gappedJob = () => [
            {state: 'ENQUEUED', createdAt: iso(0)},
            {state: 'PROCESSING', createdAt: iso(120000)},
            {state: 'SUCCEEDED', createdAt: iso(120100)},
        ];
        const compressed = buildTimelineModel({steps: gappedJob(), mode: 'compact', compression: 'compressed', reverse: false, now: ms(120100)});
        const linear = buildTimelineModel({steps: gappedJob(), mode: 'compact', compression: 'actual', reverse: false, now: ms(120100)});
        expect(compressed.compactRows[0].items[0].placement.isCompressed).toBe(true);
        expect(linear.compactRows[0].items[0].placement.isCompressed).toBe(false);
    });
});
