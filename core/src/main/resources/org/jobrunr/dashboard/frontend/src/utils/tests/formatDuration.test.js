import {formatDuration} from "../helper-functions";

describe('formatDuration', () => {
    it('formats durations from seconds into human-readable strings', () => {
        expect(formatDuration(3661)).toBe('1 hour, 1 minute, 1 second');
        expect(formatDuration(86400)).toBe('1 day');
        expect(formatDuration(31536000)).toBe('1 year');
    });

    it('handles multiple units', () => {
        expect(formatDuration(90061)).toBe('1 day, 1 hour, 1 minute, 1 second');
    });

    it('excludes units with zero values', () => {
        expect(formatDuration(3600)).toBe('1 hour');
    });

    it('handles singular units', () => {
        expect(formatDuration(1)).toBe('1 second');
    });

    it('handles zero', () => {
        expect(formatDuration(0)).toBe('');
    });

    it('handles negative values', () => {
        expect(formatDuration(-1)).toBe('');
    });

    it('handles large values', () => {
        expect(formatDuration(315360000)).toBe('10 years');
    });

});
