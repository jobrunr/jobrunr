import React from 'react';

class State {
    constructor() {
        this._listeners = new Map();
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

    useStatsState(obj) {
        const [stats, setStats] = React.useState(state.getStats());
        const cleanup = () => this.removeListener(obj);
        React.useEffect(() => {
            this.addListener(obj, setStats);
            return () => cleanup
            // eslint-disable-next-line react-hooks/exhaustive-deps
        }, []);
        return stats;
    }

    addListener(obj, listener) {
        this._listeners.set(obj, listener);
    }

    removeListener(obj) {
        this._listeners.delete(obj);
    }
}

const state = new State();
Object.freeze(state);

const eventSource = new EventSource(process.env.REACT_APP_SSE_URL + "/jobstats")
eventSource.onmessage = e => {
    state.setStats(JSON.parse(e.data));
};

export default state;