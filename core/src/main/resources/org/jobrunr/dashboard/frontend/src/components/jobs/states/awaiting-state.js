import {JobState} from "./job-state";

const Awaiting = ({jobState}) => {
    const from = new Date(jobState.from);
    const to = new Date(jobState.to);
    const title = "Pending " + (jobState.reason ? `- ${jobState.reason}` : "");

    return (
        <JobState state="awaiting" title={title} date={new Date(jobState.createdAt)}>
            Job is waiting to be scheduled at a time of low carbon emissions. It'll be scheduled between {from.toString()} and {to.toString()}.
        </JobState>
    )
};

export default Awaiting;