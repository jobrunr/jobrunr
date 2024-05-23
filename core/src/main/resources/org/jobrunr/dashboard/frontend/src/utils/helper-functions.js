import cronstrue from "cronstrue";

export function humanFileSize(bytes, si) {
    const thresh = si ? 1000 : 1024;
    if (Math.abs(bytes) < thresh) {
        return bytes + ' B';
    }
    const units = si
        ? ['kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
        : ['KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB'];
    let u = -1;
    do {
        bytes /= thresh;
        ++u;
    } while (Math.abs(bytes) >= thresh && u < units.length - 1);
    return bytes.toFixed(1) + ' ' + units[u];
}


export function formatDuration(seconds) {
    const duration = {
        years: Math.floor(seconds / (3600 * 24 * 365)),
        days: Math.floor((seconds % (3600 * 24 * 365)) / (3600 * 24)),
        hours: Math.floor((seconds % (3600 * 24)) / 3600),
        minutes: Math.floor((seconds % 3600) / 60),
        seconds: seconds % 60,
    };

    const parts = [];
    for (const [key, value] of Object.entries(duration)) {
        if (value > 0) {
            const unit = value === 1 ? key.slice(0, -1) : key;
            parts.push(`${value} ${unit}`);
        }
    }
    return parts.join(', ');
}


export function identifyScheduleType(scheduleExpression) {
    const parts = scheduleExpression.split(' ');

    if (parts.length === 7 || parts.length === 8) {
        return "CarbonAwareCron";
    } else if (parts.length === 5 || parts.length === 6) {
        return "Cron";
    } else if (parts.length === 1 && /^PT\d+H$/.test(scheduleExpression)) {
        return "Duration";
    } else {
        return "Unknown";
    }
}


export function formatCarbonAwareCron(carbonAwareCronStr) {
    let carbonAwareCron = parseCarbonAwareCron(carbonAwareCronStr);
    return cronstrue.toString(carbonAwareCron.cron) + ` (allowed ${formatDuration(carbonAwareCron.allowedDurationBefore)} before and ${formatDuration(carbonAwareCron.allowedDurationAfter)} after)`;
}


export function formatDurationEveryX(durationString) {
    if (!durationString) return "Invalid duration";
    const totalSeconds = convertISO8601DurationToSeconds(durationString);
    if (totalSeconds === null) return "Invalid duration";
    const formattedDuration = formatDuration(totalSeconds);
    const parts = formattedDuration.split(', ');
    return `Every ${parts.join(', ')}`;
}


export function parseCarbonAwareCron(scheduleExpression) {
    const parts = scheduleExpression.split(' ');
    const lastIndex = parts.length - 1;
    const duration1 = parts[lastIndex - 1];
    const duration2 = parts[lastIndex];

    parts.splice(lastIndex - 1, 2);
    const cronExpression = parts.join(' ');

    return {
        cron: cronExpression,
        allowedDurationBefore: convertISO8601DurationToSeconds(duration1),
        allowedDurationAfter: convertISO8601DurationToSeconds(duration2)
    };
}


export function convertISO8601DurationToSeconds(durationString) {
    if (!durationString) return null;
    const iso8601TimePattern = /^PT(?:(\d+)D)?(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d{1,6})?)S)?$/;
    const stringParts = iso8601TimePattern.exec(durationString);
    if (!stringParts) return null;

    return calculateTotalSeconds(stringParts);
}


function calculateTotalSeconds(stringParts) {
    const days = getSeconds(stringParts[1], 86400);
    const hours = getSeconds(stringParts[2], 3600);
    const minutes = getSeconds(stringParts[3], 60);
    const seconds = getSeconds(stringParts[4], 1);
    return days + hours + minutes + seconds;
}


function getSeconds(value, multiplier) {
    if (value === undefined) return 0;
    return parseInt(value, 10) * multiplier;
}