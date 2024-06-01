import {humanFileSize, humanFileSizeNotSiUnits} from '../helper-functions';

describe('humanFileSize', () => {
    it('returns bytes correctly for small numbers', () => {
        expect(humanFileSize(500)).toBe('500 B');
        expect(humanFileSizeNotSiUnits(1023)).toBe('1023 B');
    });

    it('converts bytes to human-readable form using SI units', () => {
        expect(humanFileSize(1000)).toBe('1.0 kB');
        expect(humanFileSize(1500)).toBe('1.5 kB');
    });

    it('converts bytes to human-readable form using binary units', () => {
        expect(humanFileSizeNotSiUnits(1024)).toBe('1.0 KiB');
        expect(humanFileSizeNotSiUnits(1536)).toBe('1.5 KiB');
    });

    it('converts bytes to human-readable form using SI units for large numbers', () => {
        expect(humanFileSize(1000000)).toBe('1.0 MB');
        expect(humanFileSize(1500000)).toBe('1.5 MB');
        expect(humanFileSize(1000000000)).toBe('1.0 GB');
        expect(humanFileSize(1500000000)).toBe('1.5 GB');
        expect(humanFileSize(1000000000000)).toBe('1.0 TB');
        expect(humanFileSize(1500000000000)).toBe('1.5 TB');
        expect(humanFileSize(1000000000000000)).toBe('1.0 PB');
        expect(humanFileSize(1500000000000000)).toBe('1.5 PB');
        expect(humanFileSize(1000000000000000000)).toBe('1.0 EB');
        expect(humanFileSize(1500000000000000000)).toBe('1.5 EB');
        expect(humanFileSize(1000000000000000000000)).toBe('1.0 ZB');
        expect(humanFileSize(1500000000000000000000)).toBe('1.5 ZB');
        expect(humanFileSize(1000000000000000000000000)).toBe('1.0 YB');
        expect(humanFileSize(1500000000000000000000000)).toBe('1.5 YB');
    });

    it('converts bytes to human-readable form using binary units for large numbers', () => {
        expect(humanFileSizeNotSiUnits(1073741824)).toBe('1.0 GiB');
        expect(humanFileSizeNotSiUnits(1610612736)).toBe('1.5 GiB');
        expect(humanFileSizeNotSiUnits(1099511627776)).toBe('1.0 TiB');
        expect(humanFileSizeNotSiUnits(1649267441664)).toBe('1.5 TiB');
        expect(humanFileSizeNotSiUnits(1125899906842624)).toBe('1.0 PiB');
        expect(humanFileSizeNotSiUnits(1688849860263936)).toBe('1.5 PiB');
        expect(humanFileSizeNotSiUnits(1152921504606846976)).toBe('1.0 EiB');
        expect(humanFileSizeNotSiUnits(1729382256910270464)).toBe('1.5 EiB');
        expect(humanFileSizeNotSiUnits(1180591620717411303424)).toBe('1.0 ZiB');
        expect(humanFileSizeNotSiUnits(1770887436805219363840)).toBe('1.5 ZiB');
        expect(humanFileSizeNotSiUnits(1208925819614629174706176)).toBe('1.0 YiB');
        expect(humanFileSizeNotSiUnits(1813388729428529952069264)).toBe('1.5 YiB');
    });
});
