import {useSyncExternalStore} from "react";

let dateStyle = localStorage.getItem('switchableTimeAgoStyle');
let dateStyleChangeListeners = [];

const getSnapshot = () => dateStyle;
const subscribe = (listener) => {
    dateStyleChangeListeners = [...dateStyleChangeListeners, listener];
    return () => {
        dateStyleChangeListeners = dateStyleChangeListeners.filter(l => l !== listener);
    };
}

export const dateStyles = {
    defaultStyle: 'defaultStyle',
    localeStyle: 'localeStyle',
    readableStyle: 'readableStyle',
    iso8601Style: 'iso8601Style'
};

export const setDateStyle = (style) => {
    localStorage.setItem('switchableTimeAgoStyle', style);
    dateStyle = style;
    dateStyleChangeListeners.forEach(listener => listener());
}

export const useDateStyles = () => {
    const style = useSyncExternalStore(subscribe, getSnapshot);
    return [style, setDateStyle];
}