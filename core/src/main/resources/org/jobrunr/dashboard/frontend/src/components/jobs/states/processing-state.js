import { useState } from "react";
import { styled } from "@mui/material/styles";
import Typography from "@mui/material/Typography";
import Accordion from "@mui/material/Accordion";
import AccordionSummary from "@mui/material/AccordionSummary";
import AccordionDetails from "@mui/material/AccordionDetails";
import ExpandMore from "@mui/icons-material/ExpandMore";
import LinearProgress, {linearProgressClasses} from '@mui/material/LinearProgress';
import Alert from '@mui/material/Alert';
import TimeAgo from "react-timeago/lib";
import {Cogs} from "mdi-material-ui";
import SwitchableTimeAgo from "../../utils/time-ago";

const Console = styled("div")(() => ({
    boxSizing: 'border-box',
    width: '100%',
    color: '#fff',
    backgroundColor: '#113462',
    padding: '24px 0 24px 24px',
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

const classes = {
    primaryHeading: {
        textTransform: "none",
        lineHeight: "inherit"
    },
    secondaryHeading: {
        alignSelf: 'center',
        marginLeft: 'auto'
    },
    alert: {
        padding: 0
    },
    expansionPanel: {
        flexDirection: 'column',
        padding: 0
    },
    processing: {
        color: "rgb(102, 60, 0)",
        backgroundColor: "rgb(255, 244, 229)",
        minHeight: 56
    },
    details: {
        padding: '24px 0 24px 24px'
    }
};

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

const Processing = (props) => {
    const index = props.index;
    const job = props.job;
    const jobState = props.jobState;
    const [expanded, setExpanded] = useState(job.jobHistory.length === (index + 1));
    const logs = getLogs(job, index);
    const progressBar = getProgressBar(job, index);
    const processingIcon = <Cogs/>

    const handleChange = () => {
        setExpanded(!expanded);
    };

    return (
        <Accordion expanded={expanded} onChange={handleChange}>
            <AccordionSummary
                style={classes.processing}
                id="processing-panel-header"
                expandIcon={<ExpandMore/>}
                aria-controls="processing-panel-content"
            >
                <Alert style={classes.alert} severity="warning" icon={processingIcon}>
                    <Typography style={classes.primaryHeading} variant="h6">
                        Processing job
                    </Typography>
                </Alert>
                <Typography style={classes.secondaryHeading}>
                    <SwitchableTimeAgo date={new Date(jobState.createdAt)} />
                </Typography>
            </AccordionSummary>
            <AccordionDetails style={classes.expansionPanel}>
                {progressBar &&
                <ColoredLinearProgress variant="determinate" value={progressBar.progress}/>
                }
                <div style={classes.details}>
                    Job is processing on server <code style={{fontSize: 'large'}}>{jobState.serverId} {jobState.serverName && <> ({jobState.serverName})</>}</code>
                </div>
                {logs.length > 0 &&
                <Console>
                    {logs.map((log) => (
                        <dl key={log.logInstant} className={log.level}>
                            <dt><TimeAgo date={new Date(log.logInstant)} now={() => new Date(jobState.createdAt)}
                                         live="false" formatter={(a, b, c) => a > 1 ? `+${a} ${b}s` : `+${a} ${b}`}/>
                            </dt>
                            <dd>{log.logMessage}</dd>
                        </dl>
                    ))}
                </Console>
                }
            </AccordionDetails>
        </Accordion>
    )
};

export default Processing;