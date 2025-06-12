import {JobState} from "./job-state";

const Deleted = ({jobState}) => {
    return (
        <JobState
            title="Job Deleted"
            state="deleted"
            date={jobState.createdAt}
            canExpand={!!jobState.reason}
        >
            {jobState.reason &&
                <div>{jobState.reason}</div>
            }
        </JobState>
    )
};

export default Deleted;