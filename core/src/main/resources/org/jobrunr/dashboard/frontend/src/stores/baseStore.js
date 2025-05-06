export default class BaseStore {
    #listeners = [];

    subscribe(listener) {
        this.#listeners = [...this.#listeners, listener];
        return () => {
            this.#listeners = this.#listeners.filter(l => l !== listener);
        }
    }

    notify() {
        this.#listeners.forEach(listener => listener());
    }
}