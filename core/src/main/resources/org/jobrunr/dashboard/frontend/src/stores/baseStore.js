export default class BaseStore {
    #listeners = [];

    subscribe(listener) {
        this.#listeners = [...this.#listeners, listener];
        return () => this.#listeners.filter(listener => listener !== listener);
    }

    notify() {
        this.#listeners.forEach(listener => listener());
    }
}