import ExpansionPanel from "@material-ui/core/ExpansionPanel";
import ExpansionPanelSummary from "@material-ui/core/ExpansionPanelSummary";
import ExpansionPanelDetails from "@material-ui/core/ExpansionPanelDetails";
import ExpandMore from "@material-ui/icons/ExpandMore";
import Alert from "@material-ui/lab/Alert";
import Typography from "@material-ui/core/Typography";
import React from "react";
import {makeStyles} from "@material-ui/core/styles";
import TimeAgo from "react-timeago/lib";
import {Check} from "mdi-material-ui";

const useStyles = makeStyles(theme => ({
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
    success: {
        color: "rgb(30, 70, 32)",
        backgroundColor: "rgb(237, 247, 237)",
        minHeight: 56,
        '& > .MuiExpansionPanelSummary-content.Mui-expanded': {
            margin: '12px 0',
        },
        '&$expanded': {
            margin: 0,
            minHeight: 56,
        },
    }
}));


function convertISO8601ToSeconds(durationString) {
    var stringPattern = /^PT(?:(\d+)D)?(?:(\d+)H)?(?:(\d+)M)?(?:(\d+(?:\.\d{1,3})?)S)?$/;
    var stringParts = stringPattern.exec(durationString);
    return (
        (
            (
                (stringParts[1] === undefined ? 0 : stringParts[1] * 1)  /* Days */
                * 24 + (stringParts[2] === undefined ? 0 : stringParts[2] * 1) /* Hours */
            )
            * 60 + (stringParts[3] === undefined ? 0 : stringParts[3] * 1) /* Minutes */
        )
        * 60 + (stringParts[4] === undefined ? 0 : stringParts[4] * 1) /* Seconds */
    );
}

const Succeeded = (props) => {
    const classes = useStyles();
    const jobState = props.jobState;
    const checkIcon = <Check/>

    const latencyDuration = jobState.latencyDuration.toString().startsWith('PT') ? convertISO8601ToSeconds(jobState.latencyDuration) : jobState.latencyDuration;
    const processDuration = jobState.processDuration.toString().startsWith('PT') ? convertISO8601ToSeconds(jobState.processDuration) : jobState.processDuration;


    return (
        <ExpansionPanel>
            <ExpansionPanelSummary
                className={classes.success}
                id="succeeded-panel-header"
                expandIcon={<ExpandMore/>}
                aria-controls="succeeded-panel-content"
            >
                <Alert className={classes.alert} severity="success" icon={checkIcon}>
                    <Typography className={classes.primaryHeading} variant="h6">
                        Job processing succeeded
                    </Typography>
                </Alert>
                <Typography className={classes.secondaryHeading}><TimeAgo
                    date={new Date(jobState.createdAt)}/></Typography>
            </ExpansionPanelSummary>
            <ExpansionPanelDetails className={classes.expansionPanel}>
                <ul>
                    <li>Latency duration: {latencyDuration.toFixed(2)} s</li>
                    <li>Process duration: {processDuration.toFixed(2)} s</li>
                </ul>
            </ExpansionPanelDetails>
        </ExpansionPanel>
    )
};

export default Succeeded;