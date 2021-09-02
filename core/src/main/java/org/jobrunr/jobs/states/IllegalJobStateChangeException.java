package org.jobrunr.jobs.states;

public class IllegalJobStateChangeException extends IllegalStateException {

    private final StateName from;
    private final StateName to;

    public IllegalJobStateChangeException(StateName from, StateName to) {
        super("A job cannot change state from " + from + " to " + to + ".");
        this.from = from;
        this.to = to;
    }

    public StateName getFrom() {
        return from;
    }

    public StateName getTo() {
        return to;
    }
}
