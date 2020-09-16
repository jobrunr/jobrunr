import ExpansionPanelSummary from "@material-ui/core/ExpansionPanelSummary";
import ExpandMore from "@material-ui/icons/ExpandMore";
import Alert from "@material-ui/lab/Alert";
import Typography from "@material-ui/core/Typography";
import ExpansionPanelDetails from "@material-ui/core/ExpansionPanelDetails";
import ExpansionPanel from "@material-ui/core/ExpansionPanel";
import React from "react";
import {makeStyles} from "@material-ui/core/styles";
import TimeAgo from "react-timeago/lib";

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
    failed: {
        color: "rgb(97, 26, 21)",
        backgroundColor: "rgb(253, 236, 234)",
        minHeight: 56,
        '& > .MuiExpansionPanelSummary-content.Mui-expanded': {
            margin: '12px 0',
        },
        '&$expanded': {
            margin: 0,
            minHeight: 56,
        },
    },
    expansionPanel: {
        display: "block"
    },
    exceptionClass: {
        textTransform: "none"
    },
    stackTrace: {
        whiteSpace: "pre-wrap",
        wordWrap: "break-word"
    }
}));


const Failed = (props) => {
    const classes = useStyles();
    const jobState = props.jobState;

    return (
        <ExpansionPanel className={classes.root}>
            <ExpansionPanelSummary
                className={classes.failed}
                id="failed-panel-header"
                expandIcon={<ExpandMore/>}
                aria-controls="failed-panel-content"
            >
                <Alert className={classes.alert} severity="error">
                    <Typography className={classes.primaryHeading} variant="h6">
                        Job processing failed - {jobState.message}
                    </Typography>
                </Alert>
                <Typography className={classes.secondaryHeading}>
                    <TimeAgo date={new Date(jobState.createdAt)} title={new Date(jobState.createdAt).toString()}/>
                </Typography>
            </ExpansionPanelSummary>

            <ExpansionPanelDetails className={classes.expansionPanel}>
                <Typography variant="h6" className={classes.exceptionClass} gutterBottom>
                    {jobState.exceptionType}
                </Typography>
                <Typography component={'pre'} className={classes.stackTrace}>
                    {jobState.stackTrace}
                </Typography>
            </ExpansionPanelDetails>
        </ExpansionPanel>
    )
};

export default Failed;