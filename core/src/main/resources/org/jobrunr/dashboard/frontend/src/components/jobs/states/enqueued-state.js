import {JobState} from "./job-state";

const Enqueued = ({jobState}) => {
    return (
        <JobState state="enqueued" title="Job Enqueued" date={jobState.createdAt} canExpand={false}/>
    )
};

export default Enqueued;