import {useSyncExternalStore} from 'react';
import TimeAgo from "react-timeago/lib";

const SwitchableTimeAgo = ({date}) => {

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

export default SwitchableTimeAgo;


let dateStyle = localStorage.getItem('switchableTimeAgoStyle');
let dateStyleChangeListeners = [];

const getSnapshot = () => dateStyle;

const subscribe = (listener) => {
    dateStyleChangeListeners = [...dateStyleChangeListeners, listener];
    return () => {
        dateStyleChangeListeners = dateStyleChangeListeners.filter(l => l !== listener);
    };
}

const setDateStyle = (style) => {
    localStorage.setItem('switchableTimeAgoStyle', style);
    dateStyle = style;
    dateStyleChangeListeners.forEach(listener => listener());
}

const useDateStyles = () => {
    return useSyncExternalStore(subscribe, getSnapshot);
}