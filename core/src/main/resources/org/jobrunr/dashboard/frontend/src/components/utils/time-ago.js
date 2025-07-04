import TimeAgo from "react-timeago/lib";
import {setDateStyle, useDateStyles} from "./date-styles";

const timeAgoFormatterWithoutSuffix = (a, b) => a !== 1 ? `${a} ${b}s` : `${a} ${b}`;

export const SuffixFreeTimeAgo = ({date, ...rest}) => <TimeAgo date={date} title={date.toString()} formatter={timeAgoFormatterWithoutSuffix} {...rest}/>;

export const SwitchableTimeRangeFormatter = ({from, to}) => {
    // if format == human readable && if from compare to now > 24h || to compare to now > 24h
    // then show only value closest to now => text will be: this job is scheduled at 3 days from now
    // else if format == human readable && if date1 compare to now < 24h && date 2 compare < now < 24h
    // then show as now
    // else if format == readableStyle && from && to hebben zelfde datum
    // then show as Sat Jul 05 2025 between 12:00:00 and 18:00:00 (GMT+0200)
    // else if different dates
    // then show as now
    // else if UTC format
    // then show as now
}

export const SwitchableTimeAgo = ({date}) => {

    const possibleStyles = {defaultStyle: 'defaultStyle', readableStyle: 'readableStyle', iso8601Style: 'iso8601Style'};

    const style = useDateStyles();

    const setNewStyle = (e, style) => {
        e.stopPropagation();
        setDateStyle(style);
    }

    let result = <TimeAgo onClick={e => setNewStyle(e, possibleStyles.readableStyle)} date={date} title={date.toString()}/>;
    if (style === possibleStyles.readableStyle) {
        let dateAsString = date.toString();
        result = <span onClick={e => setNewStyle(e, possibleStyles.iso8601Style)}>{dateAsString.substring(0, dateAsString.indexOf(' ('))}</span>
    }
    if (style === possibleStyles.iso8601Style) {
        result = <span onClick={e => setNewStyle(e, possibleStyles.defaultStyle)}>{date.toISOString()}</span>
    }

    return <span style={{cursor: "pointer"}}>{result}</span>;
}