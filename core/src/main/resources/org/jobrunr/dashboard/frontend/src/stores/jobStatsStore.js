import BaseStore from "./baseStore";

class JobStatsStore extends BaseStore {
    #stats = {estimation: {}, succeeded: undefined, allTimeSucceeded: 0};

    get stats() {
        return this.#stats;
    }

    set stats(stats) {
        this.#stats = stats;
        super.notify();
    }
}

const jobStatsStore = Object.freeze(new JobStatsStore());

const eventSource = new EventSource(process.env.REACT_APP_SSE_URL + "/jobstats")
eventSource.onmessage = e => {
    jobStatsStore.stats = JSON.parse(e.data);
};

export default jobStatsStore;