import {render, screen} from '@testing-library/react'
import AwaitingStateCarbonIntensityChart from "./awaiting-state-carbon-intensity-chart.js";
import {jest} from '@jest/globals';
import CarbonIntensityDataMock from "./__mocks__/carbon-intensity-data-mock.js";
import {setDateStyle} from "../../../hooks/useDateStyles.js";

describe('carbon aware intensity chart', () => {
    let _fetch;

    function mockFetch(status, data) {
        _fetch = global.fetch;
        global.fetch = jest.fn(() =>
            Promise.resolve({
                status,
                json: () => Promise.resolve(data),
            })
        );
    }

    beforeEach(() => {
        setDateStyle("iso8601Style") // ensure we're running this in UTC mode
    })

    afterEach(() => {
        global.fetch = _fetch;
    });

    it('Does not render if 404 from the server and scheduled in the past', async () => {
        mockFetch(404, null)
        const awaitingState = {
            "@class": "CarbonAwareAwaitingState",
            "state": "AWAITING",
            "from": "2025-07-11T10:00",
            "to": "2025-07-11T13:00"
        };
        const job = {
            jobHistory: [awaitingState, {
                "state": "SCHEDULED",
                "reason": "Good to go!",
                "scheduledAt": "2025-07-11T10:00"
            }]
        };

        render(<AwaitingStateCarbonIntensityChart job={job} jobState={awaitingState}/>)

        const noDataMessage = await screen.findByText(/No forecast available anymore for this date - it has been purged./i);
        expect(noDataMessage).toBeInTheDocument()
    })

    it('Time blocks start at midnight and render the first available with intensity data in the title', async () => {
        mockFetch(200, CarbonIntensityDataMock())
        const awaitingState = {
            "@class": "CarbonAwareAwaitingState",
            "state": "AWAITING",
            "from": "2025-07-11T10:00",
            "to": "2025-07-11T13:00"
        };
        const job = {
            jobHistory: [awaitingState, {
                "state": "SCHEDULED",
                "reason": "Good to go!"
            }]
        };

        render(<AwaitingStateCarbonIntensityChart job={job} jobState={awaitingState}/>)

        const time0000 = await screen.findByTitle("Time 00:00 — no data");
        const time0100 = await screen.findByTitle("Time 01:00 — no data");
        const time0200 = await screen.findByTitle("Time 02:00 — Intensity: 19 (0 = best)");
        expect(time0000).toBeInTheDocument()
        expect(time0100).toBeInTheDocument()
        expect(time0200).toBeInTheDocument()
    });

});
