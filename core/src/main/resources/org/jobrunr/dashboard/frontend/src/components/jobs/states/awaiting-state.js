import {JobState} from "./job-state";
import {SwitchableTimeAgo} from "../../utils/time-ago";

const Awaiting = ({jobState}) => {
    const title = "Job Pending " + (jobState.reason ? `- ${jobState.reason}` : "");

    return (
        <JobState state="awaiting" title={title} date={new Date(jobState.createdAt)}>
            Job is awaiting optimal low-carbon execution window between <SwitchableTimeAgo date={new Date(jobState.from)}/> and <SwitchableTimeAgo date={new Date(jobState.to)}/>.
        </JobState>
    )
};

export default Awaiting;