import {humanFileSize} from '../helper-functions';

describe('humanFileSize', () => {
    it('returns bytes correctly for small numbers', () => {
        expect(humanFileSize(500, true)).toBe('500 B');
        expect(humanFileSize(1023, false)).toBe('1023 B');
    });

    it('converts bytes to human-readable form using SI units', () => {
        expect(humanFileSize(1000, true)).toBe('1.0 kB');
        expect(humanFileSize(1500, true)).toBe('1.5 kB');
    });

    it('converts bytes to human-readable form using binary units', () => {
        expect(humanFileSize(1024, false)).toBe('1.0 KiB');
        expect(humanFileSize(1536, false)).toBe('1.5 KiB');
    });
});
