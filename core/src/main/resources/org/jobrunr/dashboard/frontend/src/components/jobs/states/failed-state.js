import Typography from "@mui/material/Typography";
import {JobState} from "./job-state";

const Failed = ({jobState}) => {
    return (
        <JobState
            title={`Job Processing Failed - ${jobState.message}`}
            state="failed"
            date={jobState.createdAt}
        >
            <Typography variant="h6" style={{textTransform: "none"}} gutterBottom>
                {jobState.exceptionType}
            </Typography>
            <Typography component={'pre'} style={{
                overflowX: "auto",
                fontFamily: "monospace",
                fontSize: "1em"
            }}>
                {jobState.stackTrace}
            </Typography>
            {jobState.message.includes("not found") &&
                <Typography style={{marginTop: '1em'}}>
                    With <a href="https://www.jobrunr.io/en/documentation/pro/" target="_blank" rel="noreferrer">JobRunr
                    Pro</a> you can test during your build pipeline if the JobParameters of your production
                    environment are still deserializable and use <a
                    href="https://www.jobrunr.io/en/documentation/pro/migrations/" target="_blank" rel="noreferrer">Job
                    Migrations</a> to keep your jobs up & running at all times.
                    Fail fast, gain time and enjoy a hassle free development workflow.
                </Typography>
            }
        </JobState>
    )
};

export default Failed;