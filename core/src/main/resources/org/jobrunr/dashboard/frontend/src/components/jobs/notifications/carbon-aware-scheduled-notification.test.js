import {render, screen} from '@testing-library/react'
import CarbonAwareScheduledNotification from "./carbon-aware-scheduled-notification.js";

describe('carbon aware scheduled notification', () => {
    it('does not show a notification if no carbon aware state', () => {
        const job = {};
        render(<CarbonAwareScheduledNotification job={job}/>)

        expect(screen.queryByText("This job is scheduled")).toBeNull()
    });

    it('shows a carbon aware processing info message with two margin dates if processing enabled', () => {
        const job = {
            jobHistory: [{
                "@class": "CarbonAwareAwaitingState",
                "state": "AWAITING",
                "from": "2025-07-11T10:00",
                "to": "2025-07-11T13:00"
            }, {
                "state": "SCHEDULED",
                "reason": "Good to go!"
            }]
        };
        render(<CarbonAwareScheduledNotification job={job}/>)

        expect(screen.getByText(/This job is scheduled Carbon Aware/i)).toBeInTheDocument()
        expect(screen.getByTitle(/Fri Jul 11 2025 10:00:00/i)).toBeInTheDocument()
        expect(screen.getByTitle(/Fri Jul 11 2025 13:00:00/i)).toBeInTheDocument()
    });

    it('shows a disabled message if the scheduled state contains the message disabled', () => {
        const job = {
            jobHistory: [{
                "@class": "CarbonAwareAwaitingState",
                "state": "AWAITING"
            }, {
                "state": "SCHEDULED",
                "reason": "Carbon aware scheduling is disabled",
                "scheduledAt": "2025-07-11T10:00"
            }]
        };
        render(<CarbonAwareScheduledNotification job={job}/>)

        // See https://testing-library.com/docs/queries/bytext/
        expect(screen.getByText(/is disabled/i)).toBeInTheDocument()
    })
});
