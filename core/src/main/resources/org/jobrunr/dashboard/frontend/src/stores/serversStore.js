import BaseStore from "./baseStore";

class ServersStore extends BaseStore {
    #servers = [];

    get servers() {
        return this.#servers;
    }

    set servers(servers) {
        this.#servers = this.#sortServers(servers);
        super.notify();
    }

    #sortServers(servers = []) {
        return [...servers].sort((a, b) => a.firstHeartbeat > b.firstHeartbeat);
    }
}

const serversStore = Object.freeze(new ServersStore());

export default serversStore;

const openEventSource = () => {
    const eventSource = new EventSource(process.env.REACT_APP_SSE_URL + "/servers");
    eventSource.addEventListener('message', e => serversStore.servers = JSON.parse(e.data));
    return () => eventSource.close();
}

export {openEventSource};