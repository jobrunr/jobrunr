import {JobNotification} from "./job-notification";

const JobDetailsNotCacheableNotification = () => {
    return (
        <JobNotification severity="error">
            <strong>Job details not cacheable!</strong>&nbsp;The analysis for this job cannot be cached as the provided
            lambda is too complex. This means that <b>enqueueing jobs take more time</b>. More info can be found on
            the <a href="https://www.jobrunr.io/en/documentation/background-methods/best-practices/" target="_blank"
                   rel="noreferrer">best practices page</a> of JobRunr.
        </JobNotification>
    )
};

export default JobDetailsNotCacheableNotification;