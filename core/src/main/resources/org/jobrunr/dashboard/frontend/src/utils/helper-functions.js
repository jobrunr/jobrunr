import cronstrue from "cronstrue";

export function humanFileSize(bytes) {
    return _humanFileSize(bytes, true);
}

export function humanFileSizeNotSiUnits(bytes) {
    return _humanFileSize(bytes, false);
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
    let timeWindow = "";
    if (carbonAwareCron.allowedDurationBefore > 0) {
        timeWindow += `allowed ${formatDuration(carbonAwareCron.allowedDurationBefore)} before`;
    }
    if (carbonAwareCron.allowedDurationAfter > 0) {
        if (timeWindow.length > 0) {
            timeWindow += ` and ${formatDuration(carbonAwareCron.allowedDurationAfter)} after`;
        } else {
            timeWindow += `allowed ${formatDuration(carbonAwareCron.allowedDurationAfter)} after`;
        }
    }
    let humanReadableCron = cronstrue.toString(carbonAwareCron.cron);
    if (timeWindow.length === 0) {
        return humanReadableCron;
    }
    return `${humanReadableCron} (${timeWindow})`;
}


export function formatDurationEveryX(durationString) {
    if (!durationString) return "Invalid duration";
    const totalSeconds = convertISO8601DurationToSeconds(durationString);
    if (totalSeconds === null) return "Invalid duration";
    const formattedDuration = formatDuration(totalSeconds);
    if (!formattedDuration) return "Invalid duration";
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

/* ------------------------------  */
/* HELPER FUNCTIONS (not exported) */

/* ------------------------------  */
function _humanFileSize(bytes, si) {
    const thresh = si ? 1000 : 1024;
    if (Math.abs(bytes) < thresh) {
        return `${bytes} B`
    }
    const units = si
        ? ['kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
        : ['KiB', 'MiB', 'GiB', 'TiB', 'PiB', 'EiB', 'ZiB', 'YiB'];
    let unitIndex = -1;
    while (Math.abs(bytes) >= thresh && unitIndex < units.length - 1) {
        bytes /= thresh;
        unitIndex++;
    }
    return `${bytes.toFixed(1)} ${units[unitIndex]}`;
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
