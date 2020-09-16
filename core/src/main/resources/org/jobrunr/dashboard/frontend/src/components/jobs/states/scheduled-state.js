import ExpansionPanelSummary from "@material-ui/core/ExpansionPanelSummary";
import ExpandMore from "@material-ui/icons/ExpandMore";
import Alert from "@material-ui/lab/Alert";
import Typography from "@material-ui/core/Typography";
import ExpansionPanelDetails from "@material-ui/core/ExpansionPanelDetails";
import ExpansionPanel from "@material-ui/core/ExpansionPanel";
import React from "react";
import {makeStyles} from "@material-ui/core/styles";
import TimeAgo from "react-timeago/lib";
import {Schedule} from "@material-ui/icons";

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
    scheduled: {
        color: "rgb(13, 60, 97)",
        backgroundColor: "#CFD8DC",
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


const Scheduled = (props) => {
    const classes = useStyles();
    const jobState = props.jobState;
    const scheduledIcon = <Schedule />;
    const scheduledDate = new Date(jobState.scheduledAt);
    const ScheduledMessage = () => {
        if (jobState.reason) {
            return <span>- {jobState.reason}</span>;
        }
        return <span></span>;
    };

    return (
        <ExpansionPanel>
            <ExpansionPanelSummary
                className={classes.scheduled}
                id="scheduled-panel-header"
                expandIcon={<ExpandMore/>}
                aria-controls="scheduled-panel-content"
            >
                <Alert className={classes.alert} severity="info" color="#CFD8DC" icon={scheduledIcon}>
                    <Typography className={classes.primaryHeading} variant="h6">
                        Job scheduled <ScheduledMessage/>
                    </Typography>
                </Alert>
                <Typography className={classes.secondaryHeading}>
                    <TimeAgo date={new Date(jobState.scheduledAt)} title={new Date(jobState.scheduledAt).toString()}/>
                </Typography>
            </ExpansionPanelSummary>
            <ExpansionPanelDetails>
                Job scheduled at {scheduledDate.toString()}
            </ExpansionPanelDetails>
        </ExpansionPanel>
    )
};

export default Scheduled;