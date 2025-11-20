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