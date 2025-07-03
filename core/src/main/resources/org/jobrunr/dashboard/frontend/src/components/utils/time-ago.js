import TimeAgo from "react-timeago/lib";
import {setDateStyle, useDateStyles} from "./date-styles";

const timeAgoFormatterWithoutSuffix = (a, b) => a !== 1 ? `${a} ${b}s` : `${a} ${b}`;

export const SuffixFreeTimeAgo = ({date, ...rest}) => <TimeAgo date={date} title={date.toString()} formatter={timeAgoFormatterWithoutSuffix} {...rest}/>;

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