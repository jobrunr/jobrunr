import {fireEvent, render, screen, within} from '@testing-library/react';
import {jest} from '@jest/globals';
import {JobProgressDisplay} from './job-progress-display.js';
import {setTimelineViewMode, timelineViewModes} from '../../hooks/useTimelineViewMode.js';

const jobWithARetryAndSteps = {
    jobHistory: [
        {state: "ENQUEUED", createdAt: "2025-09-01T10:00:00Z"},
        {state: "PROCESSING", createdAt: "2025-09-01T10:00:01Z", updatedAt: "2025-09-01T10:00:31Z", serverName: "server-1"},
        {state: "FAILED", createdAt: "2025-09-01T10:00:31Z", message: "Boom", exceptionType: "java.lang.RuntimeException"},
        {state: "SCHEDULED", createdAt: "2025-09-01T10:00:31Z", scheduledAt: "2025-09-01T10:00:40Z", reason: "Retry 1 of 10"},
        {state: "ENQUEUED", createdAt: "2025-09-01T10:00:40Z"},
        {state: "PROCESSING", createdAt: "2025-09-01T10:00:41Z", updatedAt: "2025-09-01T10:00:50Z", serverName: "server-1"},
        {state: "SUCCEEDED", createdAt: "2025-09-01T10:00:50Z", latencyDuration: 1.0, processDuration: 9.0},
    ],
    // JobContext suffixes the metadata of a step with the amount of job states, so every run is kept
    metadata: {
        "jr_step_start_prepare__2": ["java.time.Instant", "2025-09-01T10:00:02Z"],
        "jr_step_end_prepare__2": ["java.time.Instant", "2025-09-01T10:00:03Z"],
        "jr_step_prepare__2": ["java.lang.Boolean", true],
        "jr_step_start_send-email__6": ["java.time.Instant", "2025-09-01T10:00:42Z"],
        "jr_step_end_send-email__6": ["java.time.Instant", "2025-09-01T10:00:47Z"],
        "jr_step_send-email__6": ["java.lang.Boolean", true],
    }
};

const laneLabels = () => screen.getAllByRole('row').map(row => within(row).getByRole('rowheader').textContent);

describe('job progress display', () => {
    let consoleError;

    beforeEach(() => {
        setTimelineViewMode(timelineViewModes.compact);
        consoleError = jest.spyOn(console, 'error').mockImplementation(() => {
        });
    });

    afterEach(() => {
        expect(consoleError).not.toHaveBeenCalled();
        consoleError.mockRestore();
    });

    it('renders nothing for a job without history', () => {
        const {container} = render(<JobProgressDisplay job={{jobHistory: []}}/>);
        expect(container).toBeEmptyDOMElement();
    });

    it.each([timelineViewModes.compact, timelineViewModes.detailed])
    ('never shows how the runs of a step are stored in %s view', (mode) => {
        setTimelineViewMode(mode);
        const {container} = render(<JobProgressDisplay job={jobWithARetryAndSteps}/>);

        expect(container.textContent).not.toContain("__");
        [...container.querySelectorAll('[aria-label]')].forEach(el => expect(el.getAttribute('aria-label')).not.toContain("__"));
    });

    describe('compact view', () => {
        it('gives every state a single lane, no matter how often the job was retried', () => {
            render(<JobProgressDisplay job={jobWithARetryAndSteps}/>);

            expect(laneLabels()).toEqual(["Scheduled", "Enqueued", "Processing", "└prepare", "└send-email"]);
            expect(screen.getByText(/^Started at .* · 50 s · 2 attempts$/)).toBeInTheDocument();
        });

        it('explains what a lane holds instead of showing a bare count', () => {
            render(<JobProgressDisplay job={jobWithARetryAndSteps}/>);

            expect(screen.getByLabelText("Processing · 4 spans · 39 s in total")).toHaveTextContent("Processing");
            expect(screen.getByLabelText("send-email · 5 s")).toBeInTheDocument();
        });

        it('draws the states that ended an attempt on the lane of that attempt', () => {
            render(<JobProgressDisplay job={jobWithARetryAndSteps}/>);

            const processingLane = screen.getAllByRole('row')[2];
            expect(within(processingLane).getAllByRole('img').map(span => span.getAttribute('aria-label'))).toEqual([
                expect.stringMatching(/^Attempt 1 · Processing: from .* \(30 s\)$/),
                expect.stringMatching(/^Attempt 1 · Failed: at /),
                expect.stringMatching(/^Attempt 2 · Processing: from .* \(9 s\)$/),
                expect.stringMatching(/^Attempt 2 · Succeeded: at /),
            ]);
        });

        it('gives every step a lane that holds all of its runs', () => {
            render(<JobProgressDisplay job={jobWithARetryAndSteps}/>);

            const [prepareLane, sendEmailLane] = screen.getAllByRole('row').slice(3);
            expect(within(prepareLane).getByLabelText(/^Attempt 1 · prepare: from .* \(1 s\)$/)).toBeInTheDocument();
            expect(within(sendEmailLane).getByLabelText(/^Attempt 2 · send-email: from .* \(5 s\)$/)).toBeInTheDocument();
        });

        it('marks the steps that were skipped on a retry', () => {
            render(<JobProgressDisplay job={jobWithARetryAndSteps}/>);

            expect(screen.getByLabelText("prepare skipped on attempt 2: already completed during attempt 1")).toBeInTheDocument();
            expect(screen.getByText("Step skipped")).toBeInTheDocument();
        });
    });

    describe('detailed view', () => {
        beforeEach(() => setTimelineViewMode(timelineViewModes.detailed));

        it('gives every state and every step a lane of its own', () => {
            render(<JobProgressDisplay job={jobWithARetryAndSteps}/>);

            expect(laneLabels()).toEqual([
                "Enqueued", "Processing", "└prepare", "Failed", "Scheduled",
                // during the second attempt, prepare was skipped as it already completed
                "Enqueued", "Processing", "└prepare", "└send-email", "Succeeded"
            ]);
            expect(screen.getByText("Attempt 2")).toBeInTheDocument();
        });

        it('shows a step that was skipped on the attempt it was skipped in, without a bar', () => {
            render(<JobProgressDisplay job={jobWithARetryAndSteps}/>);

            const skippedLane = screen.getAllByRole('row')[7];
            expect(within(skippedLane).getByLabelText("prepare skipped on attempt 2: already completed during attempt 1")).toBeInTheDocument();
            expect(within(skippedLane).queryByLabelText(/^Attempt 2 · prepare: /)).not.toBeInTheDocument();
        });
    });

    it('remembers the view the user selected', () => {
        const {unmount} = render(<JobProgressDisplay job={jobWithARetryAndSteps}/>);
        expect(laneLabels()).toHaveLength(5);

        fireEvent.click(screen.getByRole('button', {name: "Detailed"}));
        expect(laneLabels()).toHaveLength(10);

        unmount();
        render(<JobProgressDisplay job={jobWithARetryAndSteps}/>);
        expect(laneLabels()).toHaveLength(10);
        expect(localStorage.getItem('jobTimelineViewMode')).toBe("detailed");
    });

    it('keeps counting as long as the job did not reach a final state', () => {
        jest.useFakeTimers().setSystemTime(new Date("2025-09-01T10:00:31Z"));
        try {
            render(<JobProgressDisplay job={{jobHistory: jobWithARetryAndSteps.jobHistory.slice(0, 2)}}/>);

            expect(screen.getByLabelText(/^Processing: from .* to now \(30 s\)$/)).toBeInTheDocument();
        } finally {
            jest.useRealTimers();
        }
    });
});
