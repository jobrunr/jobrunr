import {JobState} from "./job-state";

const Awaiting = ({jobState}) => {
    const from = new Date(jobState.from);
    const to = new Date(jobState.to);
    const title = "Job Pending " + (jobState.reason ? `- ${jobState.reason}` : "");

    return (
        <JobState state="awaiting" title={title} date={new Date(jobState.createdAt)}>
            Job is awaiting optimal low-carbon execution window between {from.toString()} and {to.toString()}.
        </JobState>
    )
};

export default Awaiting;