export function jobStateToHumanReadableName(jobState) {
    switch (jobState.toUpperCase()) {
        case 'AWAITING':
            return "Pending jobs";
        case 'SCHEDULED':
            return "Scheduled jobs";
        case 'ENQUEUED':
            return "Enqueued jobs";
        case 'PROCESSING':
            return "Jobs being processed";
        case 'SUCCEEDED':
            return "Succeeded jobs";
        case 'FAILED':
            return "Failed jobs";
        case 'DELETED':
            return "Deleted jobs";
        default:
            return 'Unknown state';
    }
}

export const getJobPreviousState = (job) => job?.jobHistory[job.jobHistory.length - 2];