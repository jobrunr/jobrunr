import {render, screen, within} from '@testing-library/react';
import {jest} from '@jest/globals';
import {JobProgressDisplay} from './job-progress-display.js';

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
    metadata: {
        "jr_step_start_send-email": ["java.time.Instant", "2025-09-01T10:00:42Z"],
        "jr_step_end_send-email": ["java.time.Instant", "2025-09-01T10:00:47Z"],
        "jr_step_send-email": ["java.lang.Boolean", true],
    }
};

describe('job progress display', () => {
    let consoleError;

    beforeEach(() => {
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

    it('renders a row per state, the steps and how long the job took', () => {
        render(<JobProgressDisplay job={jobWithARetryAndSteps}/>);

        const rows = screen.getAllByRole('row');
        expect(rows.map(row => within(row).getByRole('rowheader').textContent)).toEqual([
            "Enqueued", "Processing", "Failed", "Scheduled", "Enqueued", "Processing", "└send-email", "Succeeded"
        ]);
        expect(screen.getByText(/^Started at .* · 50 s · 2 attempts$/)).toBeInTheDocument();
        expect(screen.getByText("Attempt 2")).toBeInTheDocument();
    });

    it('describes every bar for screen readers', () => {
        render(<JobProgressDisplay job={jobWithARetryAndSteps}/>);

        expect(screen.getByLabelText(/^Attempt 1 · Processing: from .* \(30 s\)$/)).toBeInTheDocument();
        expect(screen.getByLabelText(/^Attempt 2 · send-email: from .* \(5 s\)$/)).toBeInTheDocument();
        expect(screen.getByLabelText(/^Attempt 2 · Succeeded: at /)).toBeInTheDocument();
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
