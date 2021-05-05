class ServersState {
    constructor() {
        this._listeners = [];
        this._data = {};
    }

    setServers(servers) {
        this._data.servers = servers;
        this._listeners.forEach(listener => listener(servers));
    }

    getServers() {
        return this._data.servers;
    }

    addListener(listener) {
        this._listeners.push(listener);
    }

    removeListener(listener) {
        const index = this._listeners.indexOf(listener);
        if (index > -1) {
            this._listeners.splice(index, 1);
        }
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