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

export const setDateStyle = (style) => {
    localStorage.setItem('switchableTimeAgoStyle', style);
    dateStyle = style;
    dateStyleChangeListeners.forEach(listener => listener());
}

export const useDateStyles = () => {
    return useSyncExternalStore(subscribe, getSnapshot);
}