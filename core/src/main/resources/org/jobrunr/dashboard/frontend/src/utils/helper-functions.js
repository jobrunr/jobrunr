export function humanFileSize(bytes, si = true) {
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

export function convertISO8601DurationToSeconds(durationString) {
    const stringParts = getComponentsOfISO8601Duration(durationString);
    return (
        (
            (
                (stringParts[1] === undefined ? 0 : stringParts[1] * 1)  /* Days */
                * 24 + (stringParts[2] === undefined ? 0 : stringParts[2] * 1) /* Hours */
            )
            * 60 + (stringParts[3] === undefined ? 0 : stringParts[3] * 1) /* Minutes */
        )
        * 60 + (stringParts[4] === undefined ? 0 : stringParts[4] * 1) /* Seconds */
    );
}

export function humanReadableISO8601Duration(durationString) {
    const stringParts = getComponentsOfISO8601Duration(durationString);
    if (!stringParts) return "";
    let result = "";
    if (+stringParts[1]) result += stringParts[1] + " day(s) ";
    if (+stringParts[2]) result += stringParts[2] + " hr ";
    if (+stringParts[3]) result += stringParts[3] + " min ";
    if (+stringParts[4]) result += stringParts[4] + " sec ";
    return result.trim();
}

export function humanReadableMillis(ms, significantUnits = 1) {
    const parts = getDaysHoursMinutesAndSecondsFromMillis(ms);
    const units = [
        {value: parts.days, label: "d"},
        {value: parts.hours, label: "h"},
        {value: parts.minutes, label: "m"},
        {value: parts.seconds, label: "s"},
        {value: parts.milliseconds, label: "ms"},
    ];

    const start = units.findIndex(u => u.value > 0);
    if (start === -1) return "0s";

    let resultString = "";
    for (let i = start; i < units.length && (i - start) < significantUnits; i++) {
        const unit = units[i];
        if (unit.value > 0) {
            resultString += unit.value + unit.label + " ";
        }
    }
    return resultString.trim() || "0s";
}

const getDaysHoursMinutesAndSecondsFromMillis = (ms) => {
    const totalSeconds = (ms / 1000).toFixed(3);
    const days = Math.floor(totalSeconds / 86_400);
    const hours = Math.floor((totalSeconds - (days * 86_400)) / 3600);
    const minutes = Math.floor((totalSeconds - (days * 86_400) - (hours * 3600)) / 60);
    const remainingSeconds = totalSeconds - (days * 86_400) - (hours * 3600) - (minutes * 60);
    const seconds = Math.floor(remainingSeconds);
    const milliseconds = Math.round((remainingSeconds - seconds) * 1000);
    return {days, hours, minutes, seconds, milliseconds};
};

const decimalNumberFormatter = new Intl.NumberFormat("en", {notation: "compact"});

export function humanReadableNumber(num) {
    if (typeof num !== 'number' || isNaN(num)) {
        return '?';
    }
    return decimalNumberFormatter.format(num);
}

export function parseScheduleExpression(scheduleExpressionWithOptionalCarbonAwareMargin) {
    const scheduleExpressionPattern = /(.+?)\s+\[\s*(PT(?:\d+D)?(?:\d+H)?(?:\d+M)?(?:\d+(?:\.\d{1,6})?S)?)\s*\/\s*(PT(?:\d+D)?(?:\d+H)?(?:\d+M)?(?:\d+(?:\.\d{1,6})?S)?)\s*]\s*/;

    const matches = scheduleExpressionPattern.exec(scheduleExpressionWithOptionalCarbonAwareMargin);

    const scheduleExpression = matches ? matches[1] : scheduleExpressionWithOptionalCarbonAwareMargin;
    const marginBefore = matches?.[2];
    const marginAfter = matches?.[3];

    return {scheduleExpression, marginBefore, marginAfter};
}

function getComponentsOfISO8601Duration(durationString) {
    const iso8601TimePattern = /^P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d{1,6})?)S)?)?$/;
    return iso8601TimePattern.exec(durationString);
}

export function subDaysToDate(date, days = 30) {
    const jsDate = new Date(date);
    jsDate.setDate(jsDate.getDate() - days);
    return jsDate;
}

export function convertToBrowserDefaultDateStyle(date) {
    const dateString = date.toString();
    return dateString.toString().substring(0, dateString.indexOf(' ('));
}

export function stringToColor(text) {
    let hash = 0;
    for (let i = 0; i < text.length; i++) {
        hash = text.charCodeAt(i) + ((hash << 5) - hash);
    }
    const c = (hash & 0x00FFFFFF)
        .toString(16)
        .toUpperCase();

    return "#" + "00000".substring(0, 6 - c.length) + c;
}

export const dateAsMilliseconds = (date) => new Date(date).getTime();

const padDate = (date) => {
    let cleanedDate = date.toString().replace("Z", "");
    if (!cleanedDate.includes(".")) cleanedDate += ".0";
    if (!cleanedDate.includes("T")) cleanedDate = cleanedDate.replace(" ", "T");
    cleanedDate = cleanedDate.padEnd(35, "0");
    return cleanedDate;
}

export const comparePreciseDates = (firstDate, secondDate) => {
    if (firstDate === undefined) return 1;
    if (secondDate === undefined) return -1;
    const firstDatePadded = padDate(firstDate);
    const secondDatePadded = padDate(secondDate);

    return firstDatePadded.localeCompare(secondDatePadded, "en", {sensitivity: "base"});
};

export const formatDuration = (startMs, endMs, significantUnits = 1) => {
    const ms = Math.max(0, endMs - startMs);
    if (!Number.isFinite(ms) || ms <= 0) return '<1 ms';
    return humanReadableMillis(ms, significantUnits);
};