class StatsState {
    constructor() {
        this._listeners = [];
        this._data = {};
        this._data.stats = {estimation: {}};
    }

    setStats(stats) {
        this._data.stats = stats;
        this._listeners.forEach(listener => listener(stats));
    }

    getStats() {
        return this._data.stats;
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

const statsState = new StatsState();
Object.freeze(statsState);

const eventSource = new EventSource(process.env.REACT_APP_SSE_URL + "/jobstats")
eventSource.onmessage = e => {
    statsState.setStats(JSON.parse(e.data));
};

export default statsState;