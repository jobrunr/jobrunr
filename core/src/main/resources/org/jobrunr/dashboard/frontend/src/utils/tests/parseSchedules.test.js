import {formatCarbonAwareCron, formatDurationEveryX, parseCarbonAwareCron} from '../helper-functions';

describe('parseCarbonAwareCron', () => {
    it('parses carbon-aware cron expressions and converts durations to seconds', () => {
        expect(parseCarbonAwareCron("0 0 * * 1 PT24H PT72H")).toEqual({
            cron: "0 0 * * 1",
            allowedDurationBefore: 86400,
            allowedDurationAfter: 259200
        });
        expect(parseCarbonAwareCron("0 0 * * 1 PT0S PT0S")).toEqual({
            cron: "0 0 * * 1",
            allowedDurationBefore: 0,
            allowedDurationAfter: 0
        });
        expect(parseCarbonAwareCron("0 0 * * 1 PT0S PT24H")).toEqual({
            cron: "0 0 * * 1",
            allowedDurationBefore: 0,
            allowedDurationAfter: 86400
        });
    });
});

describe('formatCarbonAwareCron', () => {
    it('formats a carbon-aware cron string correctly', () => {
        const cases = [
            {input: "0 15 10 * * ? PT2H PT1H", expected: "At 10:15 AM (allowed 2 hours before and 1 hour after)"},
            {input: "0 0 * * 1 PT0S PT72H", expected: "At 12:00 AM, only on Monday (allowed 3 days after)"},
            {input: "0 0 * * 1 PT24H PT0S", expected: "At 12:00 AM, only on Monday (allowed 1 day before)"},
            {input: "0 0 * * 1 PT0S PT0S", expected: "At 12:00 AM, only on Monday"}
        ];
        cases.forEach(({input, expected}) => {
            expect(formatCarbonAwareCron(input)).toBe(expected);
        });
    });
});

describe('formatDurationEveryX', () => {
    it('formats valid ISO 8601 duration strings correctly', () => {
        const cases = [
            {input: 'PT1H', expected: 'Every 1 hour'},
            {input: 'PT2H30M', expected: 'Every 2 hours, 30 minutes'},
            {input: 'PT27H', expected: 'Every 1 day, 3 hours'},
            {input: 'PT15M', expected: 'Every 15 minutes'},
            {input: 'PT168H', expected: 'Every 7 days'},
            {input: 'PT72H90M', expected: 'Every 3 days, 1 hour, 30 minutes'}
        ];
        cases.forEach(({input, expected}) => {
            expect(formatDurationEveryX(input)).toEqual(expected);
        });
    });

    it('returns "Invalid duration" for non-standard or malformed input', () => {
        const invalidInputs = ['', 'P2S', '2 hours', 'PT'];
        invalidInputs.forEach(input => {
            expect(formatDurationEveryX(input)).toBe('Invalid duration');
        });
    });
});

