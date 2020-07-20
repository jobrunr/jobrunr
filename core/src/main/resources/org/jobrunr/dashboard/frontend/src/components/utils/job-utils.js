export function jobStateToHumanReadableName(jobState) {
    switch (jobState.toUpperCase()) {
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
            return 'Unknown state'
    }
}