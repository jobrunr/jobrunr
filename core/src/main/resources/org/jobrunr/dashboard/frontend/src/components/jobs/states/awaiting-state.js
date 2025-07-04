import {JobState} from "./job-state";
import {SwitchableTimeRangeFormatter} from "../../utils/time-ago";
import CarbonIntensityChart from "./awaiting-state-carbon-intensity-chart";

const Awaiting = ({jobState}) => {
    const title = "Job Pending " + (jobState.reason ? `- ${jobState.reason}` : "");

    return (
        <JobState state="awaiting" title={title} date={new Date(jobState.createdAt)}>
            Job is awaiting optimal low-carbon execution window <SwitchableTimeRangeFormatter from={new Date(jobState.from)} to={new Date(jobState.to)}/>.
            <CarbonIntensityChart jobState={jobState}/>
        </JobState>
    )
};

export default Awaiting;