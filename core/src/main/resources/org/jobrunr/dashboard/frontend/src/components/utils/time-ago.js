import TimeAgo from "react-timeago";
import {dateStyles, setDateStyle, useDateStyles} from "../../hooks/useDateStyles.js";
import {convertToBrowserDefaultDateStyle} from "../../utils/helper-functions.js";

const timeAgoFormatterWithoutSuffix = (a, b) => a !== 1 ? `${a} ${b}s` : `${a} ${b}`;

export const SuffixFreeTimeAgo = ({date, ...rest}) => <TimeAgo date={date} title={date.toString()} formatter={timeAgoFormatterWithoutSuffix} {...rest}/>;

const setNewStyle = (e, style) => {
    e.stopPropagation();
    setDateStyle(style);
}

export const extractDateFromISOString = (dateAsISOString, useUTC) => {
    const date = new Date(dateAsISOString);
    return extractDateFromDate(date, useUTC);
};

export const extractDateFromDate = (date, useUTC) => {
    const year = useUTC ? date.getUTCFullYear() : date.getFullYear();
    const month = String((useUTC ? date.getUTCMonth() : date.getMonth()) + 1).padStart(2, '0');
    const day = String(useUTC ? date.getUTCDate() : date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
};

export const extractTimeFromDate = (date, useUTC) => {
    return useUTC
        ? [date.getUTCHours(), date.getUTCMinutes(), date.getUTCSeconds()]
        : [date.getHours(), date.getMinutes(), date.getSeconds()];
};

export const formatTime = (h, m) => {
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
};

export const SwitchableTimeRangeFormatter = ({from, to}) => {
    const [style] = useDateStyles();
    const hourDiff = (a, b) => Math.floor(Math.abs(a - b) / 1000 / 60 / 60);
    const now = new Date();

    const hourDiffFrom = hourDiff(now, from);
    const hourDiffTo = hourDiff(now, to);

    if (style === dateStyles.defaultStyle && (hourDiffFrom > 24 || hourDiffTo > 24)) {
        // One of the two dates is more than 24h in the future resulting in ... day(s) from now: only display the nearest one
        const date = hourDiffFrom < hourDiffTo ? from : to;
        return (
            <SwitchableTimeFormatter date={date}/>
        )
    } else if (style === dateStyles.readableStyle && hourDiff(from, to) < 24) {
        const fromTime = from.toString().replace(from.toDateString(), "").split(" ")
        const toTime = to.toString().replace(to.toDateString(), "").split(" ")

        if (fromTime[2] === toTime[2]) {
            // Timezones are equal: return "at Sat Jul 05 2025 between 12:00:00 18:00:00 (GMT+0200)"
            return (
                <span style={{cursor: "pointer"}} onClick={e => setNewStyle(e, dateStyles.iso8601Style)}>
                    at {from.toDateString()} between {fromTime[1]} and {toTime[1]} ({fromTime[2]})
                </span>
            )
        } else {
            // Timezones differ: return "at Sat Jul 05 2025O between 12:00:00 GMT+0200 and 18:00:00 GMT+0200"
            return (
                <span style={{cursor: "pointer"}} onClick={e => setNewStyle(e, dateStyles.iso8601Style)}>
                    at {from.toDateString()} between {fromTime[1]} {fromTime[2]} and {toTime[1]} {fromTime[2]}
                </span>
            )
        }
    }

    // In all other cases, make use of the default SwitchableTimeAgo implementation.
    return (
        <span>between <SwitchableTimeFormatter date={from}/> and <SwitchableTimeFormatter date={to}/></span>
    )
}
export const SwitchableTimeFormatter = ({date}) => {
    const [style] = useDateStyles();

    let result = <TimeAgo onClick={e => setNewStyle(e, dateStyles.readableStyle)} date={date} title={date.toString()}/>;
    if (style === dateStyles.readableStyle) {
        result = <span onClick={e => setNewStyle(e, dateStyles.iso8601Style)}>{convertToBrowserDefaultDateStyle(date)}</span>
    }
    if (style === dateStyles.iso8601Style) {
        result = <span onClick={e => setNewStyle(e, dateStyles.defaultStyle)}>{date.toISOString()}</span>
    }

    return <span style={{cursor: "pointer"}}>{result}</span>;
}