import Accordion from "@material-ui/core/Accordion";
import AccordionSummary from "@material-ui/core/AccordionSummary";
import AccordionDetails from "@material-ui/core/AccordionDetails";
import ExpandMore from "@material-ui/icons/ExpandMore";
import Alert from "@material-ui/lab/Alert";
import Typography from "@material-ui/core/Typography";
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
        minHeight: 56
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
        <Accordion>
            <AccordionSummary
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
            </AccordionSummary>
            <AccordionDetails>
                Job scheduled at {scheduledDate.toString()}
            </AccordionDetails>
        </Accordion>
    )
};

export default Scheduled;