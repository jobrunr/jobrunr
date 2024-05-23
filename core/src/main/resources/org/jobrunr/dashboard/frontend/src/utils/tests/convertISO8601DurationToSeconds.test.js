import {convertISO8601DurationToSeconds} from "../helper-functions";

describe('convertISO8601DurationToSeconds', () => {
    it('converts ISO 8601 durations to seconds correctly', () => {
        expect(convertISO8601DurationToSeconds('PT24H')).toBe(86400);
        expect(convertISO8601DurationToSeconds('PT1H30M')).toBe(5400);
        expect(convertISO8601DurationToSeconds('PT1H30M10S')).toBe(5410);
        expect(convertISO8601DurationToSeconds('PT1D')).toBe(86400);
    });

    it('handles complex durations', () => {
        expect(convertISO8601DurationToSeconds('PT1D1H1M1S')).toBe(90061);
    });

    it('returns null for invalid or empty durations', () => {
        expect(convertISO8601DurationToSeconds('')).toBeNull();
        expect(convertISO8601DurationToSeconds('PT')).toBe(0);
    });

    it('converts ISO 8601 durations to seconds correctly', () => {
        expect(convertISO8601DurationToSeconds('PT24H')).toBe(86400);
        expect(convertISO8601DurationToSeconds('PT1H30M')).toBe(5400);
        expect(convertISO8601DurationToSeconds('PT1H30M10S')).toBe(5410);
        expect(convertISO8601DurationToSeconds('PT1D')).toBe(86400);
        expect(convertISO8601DurationToSeconds('PT1D1H1M1S')).toBe(90061);
    });

    it('returns null for invalid or empty durations', () => {
        expect(convertISO8601DurationToSeconds('')).toBeNull();
        expect(convertISO8601DurationToSeconds('PT')).toBe(0);
        expect(convertISO8601DurationToSeconds('PT25X')).toBe(null);
    });

});
