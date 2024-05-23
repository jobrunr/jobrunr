import {formatCarbonAwareCron, formatDurationEveryX, parseCarbonAwareCron} from '../helper-functions';

describe('parseCarbonAwareCron', () => {
    it('parses carbon-aware cron expressions and converts durations to seconds', () => {
        const result = parseCarbonAwareCron("0 0 * * 1 PT24H PT72H");
        expect(result).toEqual({
            cron: "0 0 * * 1",
            allowedDurationBefore: 86400,
            allowedDurationAfter: 259200
        });
    });
});

describe('formatCarbonAwareCron', () => {
    it('formats a carbon-aware cron string correctly', () => {
        const carbonAwareCronStr = "0 15 10 * * ? PT2H PT1H";
        const result = formatCarbonAwareCron(carbonAwareCronStr);
        const expectedString = "At 10:15 AM (allowed 2 hours before and 1 hour after)";
        expect(result).toBe(expectedString);
    });

});


describe('formatDurationEveryX', () => {
    it('formats valid ISO 8601 duration strings correctly', () => {
        // Example cases
        const cases = [
            {input: 'PT1H', expected: 'Every 1 hour'},
            {input: 'PT2H30M', expected: 'Every 2 hours, 30 minutes'},
            {input: 'PT27H', expected: 'Every 1 day, 3 hours'},
            {input: 'PT15M', expected: 'Every 15 minutes'},
            {input: 'PT168H', expected: 'Every 7 days'}
        ];

        cases.forEach(({input, expected}) => {
            expect(formatDurationEveryX(input)).toEqual(expected);
        });
    });

    it('returns "Invalid duration" for non-standard or malformed input', () => {
        expect(formatDurationEveryX('')).toBe('Invalid duration');
        expect(formatDurationEveryX('P2S')).toBe('Invalid duration');
    });

    it('handles complex duration inputs', () => {
        expect(formatDurationEveryX('PT72H90M')).toBe('Every 3 days, 1 hour, 30 minutes');
    });
});
