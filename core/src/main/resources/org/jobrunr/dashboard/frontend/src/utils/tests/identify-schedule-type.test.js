import {identifyScheduleType} from '../helper-functions';


describe('identifyScheduleType', () => {
    it('identifies normal crons', () => {
        expect(identifyScheduleType("0 10 * * 0")).toBe("Cron");
    });

    it('identifies carbon-aware crons', () => {
        expect(identifyScheduleType("0 0 * * 1 PT24H PT72H")).toBe("CarbonAwareCron");
    });

    it('identifies durations', () => {
        expect(identifyScheduleType("PT24H")).toBe("Duration");
    });

    it('identifies unknown types', () => {
        expect(identifyScheduleType("Not valid expression")).toBe("Unknown");
    });
});