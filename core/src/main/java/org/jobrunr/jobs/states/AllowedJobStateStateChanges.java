package org.jobrunr.jobs.states;

import static org.jobrunr.jobs.states.StateName.*;

public class AllowedJobStateStateChanges {

    private AllowedJobStateStateChanges() {

    }

    public static boolean isIllegalStateChange(StateName from, StateName to) {
        return !isAllowedStateChange(from, to);
    }

    public static boolean isAllowedStateChange(StateName from, StateName to) {
        switch (from) {
            case SCHEDULED:
            case AWAITING:
                return to != PROCESSING && to != AWAITING;
            case ENQUEUED:
                return to != ENQUEUED && to != AWAITING;
            case PROCESSING:
                return to == SUCCEEDED || to == FAILED || to == DELETED;
            case FAILED:
            case SUCCEEDED:
                return to == SCHEDULED || to == ENQUEUED || to == DELETED;
            case DELETED:
                return to == SCHEDULED || to == ENQUEUED;
            default:
                throw new UnsupportedOperationException("Unknown state " + from);
        }
    }
}
