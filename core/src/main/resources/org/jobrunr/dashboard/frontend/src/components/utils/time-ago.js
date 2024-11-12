import {useState} from 'react';
import TimeAgo from "react-timeago/lib";
import {useNavigate} from "react-router-dom";

const timeAgoFormatterWithoutSuffix = (a, b) => a !== 1 ? `${a} ${b}s` : `${a} ${b}`;

export const SuffixFreeTimeAgo = ({date, ...rest}) => <TimeAgo date={date} title={date.toString()} formatter={timeAgoFormatterWithoutSuffix} {...rest}/>;

export const SwitchableTimeAgo = (props) => {

    const possibleStyles = {defaultStyle: 'defaultStyle', readableStyle: 'readableStyle', iso8601Style: 'iso8601Style'};

    const [style, setStyle] = useState(localStorage.getItem('switchableTimeAgoStyle'));
    const navigate = useNavigate();

    const setNewStyle = (e, style) => {
        e.stopPropagation();
        localStorage.setItem('switchableTimeAgoStyle', style);
        setStyle(style);
        navigate(0);
    }

    let result;
    if (style === possibleStyles.readableStyle) {
        let dateAsString = props.date.toString();
        result = <span onClick={e => setNewStyle(e, possibleStyles.iso8601Style)}>{dateAsString.substring(0, dateAsString.indexOf(' ('))}</span>
    } else if (style === possibleStyles.iso8601Style) {
        result = <span onClick={e => setNewStyle(e, possibleStyles.defaultStyle)}>{props.date.toISOString()}</span>
    } else if (style === possibleStyles.defaultStyle) {
        result = <TimeAgo onClick={e => setNewStyle(e, possibleStyles.readableStyle)} date={props.date} title={props.date.toString()}/>;
    } else {
        result = <TimeAgo onClick={e => setNewStyle(e, possibleStyles.readableStyle)} date={props.date} title={props.date.toString()}/>;
    }

    return (
        <>
            {result}
        </>
    );
}