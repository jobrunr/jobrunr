import React from 'react';
import TimeAgo from "react-timeago/lib";
import {useHistory} from "react-router-dom";

const SwitchableTimeAgo = (props) => {

    const possibleStyles = {defaultStyle: 'defaultStyle', readableStyle: 'readableStyle', iso8601Style: 'iso8601Style'};

    const [style, setStyle] = React.useState(localStorage.getItem('switchableTimeAgoStyle'));
    const history = useHistory();

    const setNewStyle = (e, style) => {
        e.stopPropagation();
        localStorage.setItem('switchableTimeAgoStyle', style);
        setStyle(style);
        history.go(0);
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

export default SwitchableTimeAgo;