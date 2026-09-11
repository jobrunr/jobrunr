export const AWAITING = "AWAITING";
export const SCHEDULED = "SCHEDULED";
export const ENQUEUED = "ENQUEUED";
export const PROCESSING = "PROCESSING";
export const SUCCEEDED = "SUCCEEDED";
export const FAILED = "FAILED";
export const DELETED = "DELETED";

export const RUN_STEP_ONCE = "RUN_STEP_ONCE";

export const STATE_LABELS = {
    AWAITING: "Pending",
    SCHEDULED: "Scheduled",
    ENQUEUED: "Enqueued",
    PROCESSING: "Processing",
    SUCCEEDED: "Succeeded",
    FAILED: "Failed",
    DELETED: "Deleted",
    RUN_STEP_ONCE: 'Step (runStepOnce)',
};

export const END_STATES = [SUCCEEDED, FAILED]