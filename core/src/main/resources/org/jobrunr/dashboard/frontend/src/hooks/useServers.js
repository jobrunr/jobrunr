import {useSyncExternalStore} from "react";
import serversStore from "../stores/serversStore"

const subscribe = (listener) => serversStore.subscribe(listener);
const getSnapshot = () => serversStore.servers;
export const setServers = (servers) => serversStore.servers = servers;

export const useServers = () => {
    const stats = useSyncExternalStore(subscribe, getSnapshot);
    return [stats, setServers];
};