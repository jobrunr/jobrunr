import {useState} from "react";
import {styled} from "@mui/material/styles";
import LinearProgress, {linearProgressClasses} from '@mui/material/LinearProgress';
import {JobState} from "./job-state";
import {SuffixFreeTimeAgo} from "../../utils/time-ago";

const Console = styled("div")(() => ({
    boxSizing: 'border-box',
    width: '100%',
    color: '#fff',
    backgroundColor: '#113462',
    padding: '16px',
    '& > dl': {
        fontFamily: "'Courier New', Courier, monospace",
        fontSize: '85%',
        margin: '0',
    },
    '& > dl dt': {
        float: 'left',
        clear: 'left',
        width: '200px',
        textAlign: 'right',
        color: '#3885b7',
        margin: '-0.1em 0',
    },
    '& > dl dd': {
        margin: '-0.2em 0 -0.2em 220px'
    },
    '& > dl.WARN dd': {
        color: 'orange'
    },
    '& > dl.ERROR dd': {
        color: 'red'
    }
}));

const ColoredLinearProgress = styled(LinearProgress)(() => ({
    height: '7px',
    [`& .${linearProgressClasses.barColorPrimary}`]: {
        backgroundColor: '#78b869'
    }
}));

const getLogs = (job, index) => {
    if (job.metadata && job.metadata['jobRunrDashboardLog-' + (index + 1)]) {
        return job.metadata['jobRunrDashboardLog-' + (index + 1)].logLines;
    }
    return [];
}

const getProgressBar = (job, index) => {
    if (job.metadata && job.metadata['jobRunrDashboardProgressBar-' + (index + 1)]) {
        return job.metadata['jobRunrDashboardProgressBar-' + (index + 1)];
    }
    return null;
}

const Processing = ({index, job, jobState}) => {
    const [expanded, setExpanded] = useState(job.jobHistory.length === (index + 1));
    const logs = getLogs(job, index);
    const progressBar = getProgressBar(job, index);

    const handleChange = () => {
        setExpanded(!expanded);
    };

    return (
        <JobState
            expanded={expanded}
            onChange={handleChange}
            title="Job Processing"
            state="processing"
            date={jobState.createdAt}
            removeDetailsPadding
        >
            {progressBar &&
                <ColoredLinearProgress variant="determinate" value={progressBar.progress}/>
            }
            <div style={{padding: "16px"}}>
                Job is processing on server <code
                style={{fontSize: 'large'}}>{jobState.serverId} {jobState.serverName && <> ({jobState.serverName})</>}</code>
            </div>
            {logs.length > 0 &&
                <Console>
                    {logs.map((log) => (
                        <dl key={log.logInstant} className={log.level}>
                            <dt>
                                <SuffixFreeTimeAgo date={new Date(log.logInstant)} now={() => new Date(jobState.createdAt)} live="false"/>
                            </dt>
                            <dd>{log.logMessage}</dd>
                        </dl>
                    ))}
                </Console>
            }
        </JobState>
    )
};

export default Processing;