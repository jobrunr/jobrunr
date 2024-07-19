import {convertISO8601DurationToSeconds, humanFileSize, parseScheduleExpression} from '../helper-functions';

describe('humanFileSize', () => {
    it('returns bytes correctly for small numbers', () => {
        expect(humanFileSize(500)).toBe('500 B');
        expect(humanFileSize(1023, false)).toBe('1023 B');
    });

    it('converts bytes to human-readable form using SI units', () => {
        expect(humanFileSize(1000)).toBe('1.0 kB');
        expect(humanFileSize(1500)).toBe('1.5 kB');
    });

    it('converts bytes to human-readable form using binary units', () => {
        expect(humanFileSize(1024, false)).toBe('1.0 KiB');
        expect(humanFileSize(1536, false)).toBe('1.5 KiB');
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
        expect(humanFileSize(1073741824, false)).toBe('1.0 GiB');
        expect(humanFileSize(1610612736, false)).toBe('1.5 GiB');
        expect(humanFileSize(1099511627776, false)).toBe('1.0 TiB');
        expect(humanFileSize(1649267441664, false)).toBe('1.5 TiB');
        expect(humanFileSize(1125899906842624, false)).toBe('1.0 PiB');
        expect(humanFileSize(1688849860263936, false)).toBe('1.5 PiB');
        expect(humanFileSize(1152921504606846976, false)).toBe('1.0 EiB');
        expect(humanFileSize(1729382256910270464, false)).toBe('1.5 EiB');
        expect(humanFileSize(1180591620717411303424, false)).toBe('1.0 ZiB');
        expect(humanFileSize(1770887436805219363840, false)).toBe('1.5 ZiB');
        expect(humanFileSize(1208925819614629174706176, false)).toBe('1.0 YiB');
        expect(humanFileSize(1813388729428529952069264, false)).toBe('1.5 YiB');
    });
});

describe('convertISO8601DurationToSeconds', () => {
    it('converts ISO 8601 durations to seconds correctly', () => {
        expect(convertISO8601DurationToSeconds('PT24H')).toBe(86400);
        expect(convertISO8601DurationToSeconds('PT1H30M')).toBe(5400);
        expect(convertISO8601DurationToSeconds('PT1H30M10S')).toBe(5410);
        expect(convertISO8601DurationToSeconds('PT12H4M29.45S')).toBe(43469.45);

        expect(convertISO8601DurationToSeconds('P1D')).toBe(86400);
        expect(convertISO8601DurationToSeconds('P30D')).toBe(2592000);
        expect(convertISO8601DurationToSeconds('P1DT24H')).toBe(172800);
        expect(convertISO8601DurationToSeconds('P1DT1H1M29.45S')).toBe(90089.45);
    });
})

describe('parseScheduleExpression', () => {
    it('parses cron expressions', () => {
        expect(parseScheduleExpression('* * * * *')).toEqual({scheduleExpression: "* * * * *"});
        expect(parseScheduleExpression('0 0 * * 1')).toEqual({scheduleExpression: "0 0 * * 1"});
        expect(parseScheduleExpression('1,5,20,59 * * * * *')).toEqual({scheduleExpression: "1,5,20,59 * * * * *"});
        expect(parseScheduleExpression('*/3 * * * * *')).toEqual({scheduleExpression: "*/3 * * * * *"});
        expect(parseScheduleExpression('0 0 0 29 2 */5')).toEqual({scheduleExpression: "0 0 0 29 2 */5"});
    });

    it('parses interval expressions', () => {
        expect(parseScheduleExpression('PT24H')).toEqual({scheduleExpression: "PT24H"});
        expect(parseScheduleExpression('PT1H30M')).toEqual({scheduleExpression: "PT1H30M"});
        expect(parseScheduleExpression('PT1H30M10S')).toEqual({scheduleExpression: "PT1H30M10S"});
        expect(parseScheduleExpression('PT12H4M29.45S')).toEqual({scheduleExpression: "PT12H4M29.45S"});
    });

    it('parses carbon aware expressions', () => {
        expect(parseScheduleExpression('1,5,20,59 * * * * * [PT2H/PT7H]')).toEqual({
            scheduleExpression: "1,5,20,59 * * * * *",
            marginBefore: "PT2H",
            marginAfter: "PT7H"
        });
        expect(parseScheduleExpression('*/3 * * * * * [ PT2H / PT7H ] ')).toEqual({
            scheduleExpression: "*/3 * * * * *",
            marginBefore: "PT2H",
            marginAfter: "PT7H"
        });
        expect(parseScheduleExpression('0 0 0 29 2 */5 [ PT2H / PT12H4M29.45S]')).toEqual({
            scheduleExpression: "0 0 0 29 2 */5",
            marginBefore: "PT2H",
            marginAfter: "PT12H4M29.45S"
        });

        expect(parseScheduleExpression('PT8H30M [PT2H/PT2H]')).toEqual({
            scheduleExpression: "PT8H30M",
            marginBefore: "PT2H",
            marginAfter: "PT2H"
        });
        expect(parseScheduleExpression('PT8H30M10S [PT2H/PT3H30M2S]')).toEqual({
            scheduleExpression: "PT8H30M10S",
            marginBefore: "PT2H",
            marginAfter: "PT3H30M2S"
        });
        expect(parseScheduleExpression('PT12H4M29.45S [PT0S/PT3H30M2.30S]')).toEqual({
            scheduleExpression: "PT12H4M29.45S",
            marginBefore: "PT0S",
            marginAfter: "PT3H30M2.30S"
        });
    });
})