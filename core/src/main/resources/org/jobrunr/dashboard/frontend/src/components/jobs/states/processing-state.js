import React from "react";
import Typography from "@material-ui/core/Typography";
import ExpansionPanel from "@material-ui/core/ExpansionPanel";
import ExpandMore from "@material-ui/icons/ExpandMore";
import ExpansionPanelSummary from "@material-ui/core/ExpansionPanelSummary";
import ExpansionPanelDetails from "@material-ui/core/ExpansionPanelDetails";
import LinearProgress from '@material-ui/core/LinearProgress';
import Alert from "@material-ui/lab/Alert";
import TimeAgo from "react-timeago/lib";
import {makeStyles, withStyles} from '@material-ui/core/styles';
import {Cogs} from "mdi-material-ui";

const useStyles = makeStyles(() => ({
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
        minHeight: 56,
        '& > .MuiExpansionPanelSummary-content.Mui-expanded': {
            margin: '12px 0',
        },
        '&$expanded': {
            margin: 0,
            minHeight: 56,
        },
    },
    details: {
        padding: '24px 0 24px 24px'
    },
    console: {
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
    }
}));

const ColoredLinearProgress = withStyles({
    root: {
        height: '7px'
    },
    barColorPrimary: {
        backgroundColor: '#78b869'
    },
})(LinearProgress);

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
    const classes = useStyles();
    const index = props.index;
    const job = props.job;
    const jobState = props.jobState;
    const [expanded, setExpanded] = React.useState(job.jobHistory.length === (index + 1));
    const logs = getLogs(job, index);
    const progressBar = getProgressBar(job, index);
    const processingIcon = <Cogs/>

    const handleChange = () => {
        setExpanded(!expanded);
    };

    return (
        <ExpansionPanel expanded={expanded} onChange={handleChange}>
            <ExpansionPanelSummary
                className={classes.processing}
                id="processing-panel-header"
                expandIcon={<ExpandMore/>}
                aria-controls="processing-panel-content"
            >
                <Alert className={classes.alert} severity="warning" icon={processingIcon}>
                    <Typography className={classes.primaryHeading} variant="h6">
                        Processing job
                    </Typography>
                </Alert>
                <Typography className={classes.secondaryHeading}>
                    <TimeAgo date={new Date(jobState.createdAt)} title={new Date(jobState.createdAt).toString()}/>
                </Typography>
            </ExpansionPanelSummary>
            <ExpansionPanelDetails className={classes.expansionPanel}>
                {progressBar &&
                <ColoredLinearProgress variant="determinate" value={progressBar.progress}/>
                }
                <div className={classes.details}>Job is processing on server {jobState.serverId}</div>
                {logs.length > 0 &&
                <div className={classes.console}>
                    {logs.map((log) => (
                        <dl key={log.logInstant} className={log.level}>
                            <dt><TimeAgo date={new Date(log.logInstant)} now={() => new Date(jobState.createdAt)}
                                         live="false" formatter={(a, b, c) => a > 1 ? `+${a} ${b}s` : `+${a} ${b}`}/>
                            </dt>
                            <dd>{log.logMessage}</dd>
                        </dl>
                    ))}
                </div>
                }
            </ExpansionPanelDetails>
        </ExpansionPanel>
    )
};

export default Processing;