import {JobState} from "./job-state";

const Awaiting = ({jobState}) => {
    const from = new Date(jobState.from);
    const to = new Date(jobState.to);
    const title = "Awaiting between " + from.toString() + " and " + to.toString();

    return (
        <JobState state="awaiting" title={title} from={jobState.from} to={jobState.to}>
            Job is waiting. Will be scheduled between {from.toString()} and {to.toString()}
        </JobState>
    )
};

export default Awaiting;