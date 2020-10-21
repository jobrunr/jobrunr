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

export function convertISO8601DurationToSeconds(durationString) {
    const iso8601TimePattern = /^PT(?:(\d+)D)?(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d{1,6})?)S)?$/;
    const stringParts = iso8601TimePattern.exec(durationString);
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