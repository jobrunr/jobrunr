import {
    buildTimelineModel,
    createTimeCompressor,
    EXCLUDED_STATES,
    generateTimeTicks,
    getStepLabel,
    groupCompactStepsSequentially,
    removeInitialScheduled,
} from './timeline-data.js';
import {createJobExecutionTimelineEntries} from './timeline-entries.js';
import {FAILED, SUCCEEDED} from "../../../utils/state-names.js";

const BASE = Date.UTC(2024, 0, 1, 0, 0, 0);
const iso = (offsetMs) => new Date(BASE + offsetMs).toISOString();
const ms = (offsetMs) => BASE + offsetMs;

const timelineItem = (state, startMs, endMs, extra = {}) => ({
    state,
    label: extra.label ?? state,
    startMs,
    endMs,
    active: false,
    isPoint: false,
    isSkipped: false,
    ...extra,
});

describe('removeInitialScheduled', () => {
    it('removes a single leading SCHEDULED entry and leaves the rest untouched', () => {
        const steps = [{state: 'SCHEDULED'}, {state: 'ENQUEUED'}, {state: 'SCHEDULED'}];
        expect(removeInitialScheduled(steps)).toEqual([{state: 'ENQUEUED'}, {state: 'SCHEDULED'}]);
    });

    it('does nothing when the first entry is not SCHEDULED', () => {
        const steps = [{state: 'ENQUEUED'}, {state: 'SCHEDULED'}];
        expect(removeInitialScheduled(steps)).toBe(steps);
    });

    it('handles empty and nullish input', () => {
        expect(removeInitialScheduled([])).toEqual([]);
        expect(removeInitialScheduled(null)).toEqual([]);
    });
});

describe('getStepLabel', () => {
    it('uses the entry label first', () => {
        expect(getStepLabel({state: 'RUN_STEP_ONCE', label: 'my-step'})).toBe('my-step');
    });

    it('falls back to the state label, then the raw state, then Unknown', () => {
        expect(getStepLabel({state: 'ENQUEUED'})).toBe('Enqueued');
        expect(getStepLabel({state: 'WEIRD'})).toBe('WEIRD');
        expect(getStepLabel({})).toBe('Unknown');
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
    it('groups lifecycle states into rows and keeps steps in their own rows', () => {
        const items = [
            timelineItem('ENQUEUED', ms(0), ms(100)),
            timelineItem('PROCESSING', ms(100), ms(10000), {outcome: 'SUCCEEDED'}),
            timelineItem('RUN_STEP_ONCE', ms(200), ms(300), {label: 'stepA', isStep: true}),
            timelineItem('RUN_STEP_ONCE', ms(22000), ms(22000), {label: 'stepA', isSkipped: true, isPoint: true}),
        ];
        const rows = groupCompactStepsSequentially(items);
        expect(rows.map((row) => row.key)).toEqual(['ENQUEUED', 'PROCESSING', 'stepA']);
        expect(rows.find((row) => row.key === 'PROCESSING').items[0].outcome).toBe('SUCCEEDED');
        expect(rows.find((row) => row.key === 'PROCESSING').totalMs).toBe(ms(10000) - ms(100));
        expect(rows.find((row) => row.key === 'stepA').totalMs).toBe(100);
    });
});

describe('buildTimelineModel', () => {
    const completedEntries = () => [
        {state: 'ENQUEUED', label: 'Enqueued', startedAt: iso(0), finishedAt: iso(100), type: 'span'},
        {state: 'PROCESSING', label: 'Processing', startedAt: iso(100), finishedAt: iso(10000), type: 'span', outcome: 'SUCCEEDED'},
        {state: 'SUCCEEDED', label: 'Succeeded', startedAt: iso(10000), finishedAt: iso(10000), type: 'milestone'},
    ];

    it('returns a populated model with ticks and rows carrying placement data', () => {
        const model = buildTimelineModel({steps: completedEntries(), timelineMode: 'compact', compressionMode: 'compressed', reverse: false, now: ms(10000)});
        expect(model.start).toBe(ms(0));
        expect(model.end).toBe(ms(10000));
        expect(model.ticks.length).toBeGreaterThan(0);
        expect(model.compactRows.map((row) => row.key)).toEqual(['ENQUEUED', 'PROCESSING']);
        const placement = model.compactRows[0].items[0].placement;
        expect(placement.offset).toBeGreaterThanOrEqual(0);
        expect(Number.isFinite(placement.width)).toBe(true);
        expect(model.orderedDetailedRows[0].item.state).toBe('ENQUEUED');
    });

    it('returns null when there are no displayable entries', () => {
        const model = buildTimelineModel({steps: [], timelineMode: 'compact', compressionMode: 'compressed', reverse: false, now: ms(0)});
        expect(model).toBeNull();
    });

    it('reverses row order when reverse is true', () => {
        const model = buildTimelineModel({steps: completedEntries(), timelineMode: 'compact', compressionMode: 'compressed', reverse: true, now: ms(10000)});
        expect(model.compactRows.map((row) => row.key)).toEqual(['PROCESSING', 'ENQUEUED']);
        expect(model.orderedDetailedRows[0].item.state).toBe('SUCCEEDED');
    });

    it('marks a bar as compressed only when compression is enabled and a long range exists', () => {
        const gappedEntries = () => [
            {state: 'ENQUEUED', label: 'Enqueued', startedAt: iso(0), finishedAt: iso(120000), type: 'span'},
            {state: 'PROCESSING', label: 'Processing', startedAt: iso(120000), finishedAt: iso(120100), type: 'span'},
            {state: 'SUCCEEDED', label: 'Succeeded', startedAt: iso(120100), finishedAt: iso(120100), type: 'milestone'},
        ];
        const compressed = buildTimelineModel({
            steps: gappedEntries(),
            timelineMode: 'compact',
            compressionMode: 'compressed',
            reverse: false,
            now: ms(120100)
        });
        const linear = buildTimelineModel({steps: gappedEntries(), timelineMode: 'compact', compressionMode: 'linear', reverse: false, now: ms(120100)});
        expect(compressed.compactRows[0].items[0].placement.isCompressed).toBe(true);
        expect(linear.compactRows[0].items[0].placement.isCompressed).toBe(false);
    });

    it('keeps active entries running until now', () => {
        const ongoingEntries = () => [
            {state: 'ENQUEUED', label: 'Enqueued', startedAt: iso(0), finishedAt: iso(100), type: 'span'},
            {state: 'PROCESSING', label: 'Processing', startedAt: iso(100), finishedAt: undefined, type: 'span'},
        ];
        const model = buildTimelineModel({steps: ongoingEntries(), timelineMode: 'detailed', compressionMode: 'linear', reverse: false, now: ms(10000)});
        const processingRow = model.orderedDetailedRows[1];
        expect(processingRow.item.active).toBe(true);
        expect(processingRow.item.endMs).toBe(ms(10000));
        expect(Number.isFinite(processingRow.item.placement.width)).toBe(true);
    });
});

describe('createJobExecutionTimelineEntries + buildTimelineModel', () => {
    const job = () => ({
        jobHistory: [
            {state: 'ENQUEUED', createdAt: iso(0)},
            {state: 'PROCESSING', createdAt: iso(1000)},
            {state: 'FAILED', createdAt: iso(5000)},
            {state: 'SCHEDULED', createdAt: iso(10000), scheduledAt: iso(10500)},
            {state: 'ENQUEUED', createdAt: iso(11000)},
            {state: 'PROCESSING', createdAt: iso(11500)},
            {state: 'FAILED', createdAt: iso(15000)},
            {state: 'SCHEDULED', createdAt: iso(20000), scheduledAt: iso(20500)},
            {state: 'ENQUEUED', createdAt: iso(21000)},
            {state: 'PROCESSING', createdAt: iso(21500)},
            {state: 'SUCCEEDED', createdAt: iso(25000)},
        ],
        metadata: {
            'jr_step_start_stepA__3': iso(2000), 'jr_step_end_stepA__3': iso(3000), 'jr_step_stepA__3': true,
            'jr_step_start_stepB__3': iso(4000), 'jr_step_end_stepB__3': iso(4900), 'jr_step_stepB__3': false,
            'jr_step_start_stepB__7': iso(12000), 'jr_step_end_stepB__7': iso(13000), 'jr_step_stepB__7': true,
            'jr_step_start_stepC__11': iso(22000), 'jr_step_end_stepC__11': iso(23000), 'jr_step_stepC__11': true,
        },
    });

    const buildModel = (timelineMode, now = ms(25000)) => {
        const entries = createJobExecutionTimelineEntries(job());
        const steps = entries.filter((entry) => !EXCLUDED_STATES.includes(entry.state));
        return buildTimelineModel({steps, timelineMode, compressionMode: 'linear', reverse: false, now});
    };

    it('places every skipped step once in the row of its step, without phantom rows', () => {
        const model = buildModel('compact');
        const stepRows = model.compactRows.filter((row) => row.isStep);
        expect(stepRows.map((row) => row.key)).toEqual(['stepA', 'stepB', 'stepC']);
        expect(stepRows.map((row) => row.label)).toEqual(['stepA', 'stepB', 'stepC']);
        expect(stepRows.find((row) => row.key === 'stepA').items.map((item) => item.isSkipped)).toEqual([false, true, true]);
        expect(stepRows.find((row) => row.key === 'stepB').items.map((item) => item.isSkipped)).toEqual([false, false, true]);
        expect(stepRows.find((row) => row.key === 'stepC').items.map((item) => item.isSkipped)).toEqual([false]);
    });

    it('keeps all placements finite', () => {
        const model = buildModel('compact');
        model.compactRows.flatMap((row) => row.items).forEach((item) => {
            expect(Number.isFinite(item.placement.offset)).toBe(true);
            expect(Number.isFinite(item.placement.width)).toBe(true);
        });
    });

    it('creates retry events from the retry milestones', () => {
        const model = buildModel('compact');
        expect(model.retryEvents.map((event) => ({label: event.label, isRequeue: event.isRequeue})))
            .toEqual([{label: 'Retry 1', isRequeue: false}, {label: 'Retry 2', isRequeue: false}]);
        expect(model.retryEvents.map((event) => event.ms)).toEqual([ms(10000), ms(20000)]);
    });

    it('renders retry milestones as separators in the detailed rows', () => {
        const model = buildModel('detailed');
        const separators = model.orderedDetailedRows.filter((row) => row.isSeparator);
        expect(separators.map((row) => ({label: row.label, isRequeue: row.isRequeue})))
            .toEqual([{label: 'Retry 1', isRequeue: false}, {label: 'Retry 2', isRequeue: false}]);
    });

    it('flags requeue markers separately from retries', () => {
        const requeuedJob = () => ({
            jobHistory: [
                {state: 'ENQUEUED', createdAt: iso(0)},
                {state: 'PROCESSING', createdAt: iso(1000)},
                {state: 'FAILED', createdAt: iso(5000)},
                {state: 'SCHEDULED', createdAt: iso(6000), scheduledAt: iso(6500)},
                {state: 'ENQUEUED', createdAt: iso(7000)},
                {state: 'PROCESSING', createdAt: iso(7500)},
                {state: 'SUCCEEDED', createdAt: iso(9000)},
                {state: 'ENQUEUED', createdAt: iso(10000)},
                {state: 'PROCESSING', createdAt: iso(10500)},
                {state: 'SUCCEEDED', createdAt: iso(12000)},
            ],
            metadata: {},
        });
        const steps = createJobExecutionTimelineEntries(requeuedJob())
            .filter((entry) => !EXCLUDED_STATES.includes(entry.state));
        const build = (timelineMode) => buildTimelineModel({steps, timelineMode, compressionMode: 'linear', reverse: false, now: ms(12000)});

        expect(build('compact').retryEvents.map((event) => ({label: event.label, isRequeue: event.isRequeue})))
            .toEqual([{label: 'Retry 1', isRequeue: false}, {label: 'Requeue 1', isRequeue: true}]);
        expect(build('detailed').orderedDetailedRows.filter((row) => row.isSeparator)
            .map((row) => ({label: row.label, isRequeue: row.isRequeue})))
            .toEqual([{label: 'Retry 1', isRequeue: false}, {label: 'Requeue 1', isRequeue: true}]);
    });

    it('shows the processing outcome as a marker on the bar in compact mode only', () => {
        const compactModel = buildModel('compact');
        const compactProcessing = compactModel.compactRows.find((row) => row.key === 'PROCESSING').items;
        expect(compactProcessing.map((item) => item.outcome)).toEqual([
            {state: FAILED, result: undefined},
            {state: FAILED, result: undefined},
            {state: SUCCEEDED, result: undefined}
        ]);

        const detailedModel = buildModel('detailed');
        const detailedProcessing = detailedModel.orderedDetailedRows.filter((row) => !row.isSeparator && row.item.state === 'PROCESSING');
        expect(detailedProcessing).toHaveLength(3);
        expect(detailedProcessing.every((row) => row.item.outcome === undefined)).toBe(true);
        // why: the outcome remains visible as the end-state milestone rows that follow each attempt
        expect(detailedModel.orderedDetailedRows
            .filter((row) => !row.isSeparator && ['FAILED', 'SUCCEEDED'].includes(row.item.state))
            .map((row) => row.label)).toEqual(['Failed', 'Failed', 'Succeeded']);
    });
});
