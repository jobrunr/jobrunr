import {JobState} from "./job-state";

const Scheduled = ({jobState}) => {
    const scheduledDate = new Date(jobState.scheduledAt);
    const title = "Job Scheduled" + (jobState.reason ? `- ${jobState.reason}` : "");

    return (
        <JobState state="scheduled" title={title} date={jobState.scheduledAt}>
            Job scheduled at {scheduledDate.toString()}
        </JobState>
    )
};

export default Scheduled;