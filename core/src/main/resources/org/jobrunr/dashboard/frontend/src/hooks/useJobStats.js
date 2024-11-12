import {useSyncExternalStore} from "react";
import jobStatsStore from "../stores/jobStatsStore"

const subscribe = (listener) => jobStatsStore.subscribe(listener);
const getSnapshot = () => jobStatsStore.stats;
const setStats = (stats) => jobStatsStore.stats = stats;

export const useJobStats = () => {
    const stats = useSyncExternalStore(subscribe, getSnapshot);
    return [stats, setStats];
};