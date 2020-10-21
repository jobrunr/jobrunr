import React from 'react';

class ServersState {
    constructor() {
        this._listeners = new Map();
        this._data = {};
    }

    setServers(servers) {
        this._data.servers = servers;
        this._listeners.forEach(listener => listener(servers));
    }

    getServers() {
        return this._data.servers;
    }

    useServersState(obj) {
        const [servers, setServers] = React.useState(serversState.getServers());
        const cleanup = () => this.removeListener(obj);
        React.useEffect(() => {
            this.addListener(obj, setServers);
            return () => cleanup
            // eslint-disable-next-line react-hooks/exhaustive-deps
        }, []);
        return servers;
    }

    addListener(obj, listener) {
        this._listeners.set(obj, listener);
    }

    removeListener(obj) {
        this._listeners.delete(obj);
    }
}

const serversState = new ServersState();
Object.freeze(serversState);

fetch(`/api/servers`)
    .then(res => res.json())
    .then(response => {
        serversState.setServers(sortServers(response));
    })
    .catch(error => console.log(error));

const sortServers = (servers) => {
    servers.sort((a, b) => a.firstHeartbeat > b.firstHeartbeat)
    return servers;
}

export default serversState;