import {useSyncExternalStore} from "react";

export const timelineViewModes = {
    compact: 'compact',
    detailed: 'detailed'
};

const STORAGE_KEY = 'jobTimelineViewMode';

let viewMode = localStorage.getItem(STORAGE_KEY) ?? timelineViewModes.compact;
let viewModeChangeListeners = [];

const getSnapshot = () => viewMode;
const subscribe = (listener) => {
    viewModeChangeListeners = [...viewModeChangeListeners, listener];
    return () => {
        viewModeChangeListeners = viewModeChangeListeners.filter(l => l !== listener);
    };
}

export const setTimelineViewMode = (mode) => {
    if (!timelineViewModes[mode]) return;
    localStorage.setItem(STORAGE_KEY, mode);
    viewMode = mode;
    viewModeChangeListeners.forEach(listener => listener());
}

export const useTimelineViewMode = () => {
    const mode = useSyncExternalStore(subscribe, getSnapshot);
    return [mode, setTimelineViewMode];
}
