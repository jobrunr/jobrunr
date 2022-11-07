import Accordion from "@material-ui/core/Accordion";
import AccordionSummary from "@material-ui/core/AccordionSummary";
import Alert from "@material-ui/lab/Alert";
import Typography from "@material-ui/core/Typography";
import React from "react";
import {makeStyles} from "@material-ui/core/styles";
import {TimerSand} from "mdi-material-ui";
import SwitchableTimeAgo from "../../utils/time-ago";
import {Icon} from "@material-ui/core";


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
    info: {
        color: "rgb(13, 60, 97)",
        backgroundColor: "rgb(232, 244, 253)",
        minHeight: 56,
    }
}));


const Enqueued = (props) => {
    const classes = useStyles();
    const jobState = props.jobState;
    const enqueuedIcon = <TimerSand />

    return (
        <Accordion>
            <AccordionSummary
                className={classes.info}
                expandIcon={<Icon/>}
                id="enqueued-panel-header"
            >
                <Alert className={classes.alert} severity="info" icon={enqueuedIcon}>
                    <Typography className={classes.primaryHeading} variant="h6">
                        Job enqueued
                    </Typography>
                </Alert>
                <Typography className={classes.secondaryHeading}>
                    <SwitchableTimeAgo date={new Date(jobState.createdAt)} />
                </Typography>
            </AccordionSummary>
        </Accordion>
    )
};

export default Enqueued;