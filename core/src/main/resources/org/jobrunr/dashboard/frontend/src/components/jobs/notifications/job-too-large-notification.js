import {JobNotification} from "./job-notification";

const JobTooLargeNotification = ({}) => {
    return (
        <JobNotification severity="warning">
            <strong>This job is very large.</strong> This jobs JSON
            is very large, it exceeds 3MB. Keep job arguments small to avoid performance issues.
        </JobNotification>
    )
};

export default JobTooLargeNotification;