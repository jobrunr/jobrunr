import ExpansionPanelSummary from "@material-ui/core/ExpansionPanelSummary";
import ExpandMore from "@material-ui/icons/ExpandMore";
import Alert from "@material-ui/lab/Alert";
import Typography from "@material-ui/core/Typography";
import ExpansionPanelDetails from "@material-ui/core/ExpansionPanelDetails";
import ExpansionPanel from "@material-ui/core/ExpansionPanel";
import React from "react";
import {makeStyles} from "@material-ui/core/styles";
import TimeAgo from "react-timeago/lib";
import {Cogs} from "mdi-material-ui";

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
    }
}));


const Processing = (props) => {
    const classes = useStyles();
    const jobState = props.jobState;
    const processingIcon = <Cogs />

    return (
        <ExpansionPanel>
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
                <Typography className={classes.secondaryHeading}><TimeAgo date={new Date(jobState.createdAt)}/></Typography>
            </ExpansionPanelSummary>
            <ExpansionPanelDetails className={classes.expansionPanel}>
                Job is processing on server {jobState.serverId}
            </ExpansionPanelDetails>
        </ExpansionPanel>
    )
};

export default Processing;