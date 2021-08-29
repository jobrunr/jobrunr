import ExpansionPanelSummary from "@material-ui/core/ExpansionPanelSummary";
import ExpansionPanelDetails from "@material-ui/core/ExpansionPanelDetails";
import Alert from "@material-ui/lab/Alert";
import Typography from "@material-ui/core/Typography";
import ExpansionPanel from "@material-ui/core/ExpansionPanel";
import React from "react";
import {makeStyles} from "@material-ui/core/styles";
import TimeAgo from "react-timeago/lib";
import {Delete} from "mdi-material-ui";


const useStyles = makeStyles(() => ({
    primaryHeading: {
        textTransform: "none",
        lineHeight: "inherit"
    },
    secondaryHeading: {
        alignSelf: 'center',
        marginLeft: 'auto',
        backgroundColor: 'inherit'
    },
    details: {
        padding: '24px 0 24px 24px'
    },
    alert: {
        padding: 0
    },
    info: {
        color: '#ffe4bf',
        backgroundColor: '#654b3d',
        minHeight: 56,
        '& div.MuiAlert-icon': {
            color: '#e78f73',
            backgroundColor: '#654b3d',
        },
        '& div.MuiAlert-standardInfo': {
            color: '#ffe4bf',
            backgroundColor: '#654b3d',
        },
        '& > .MuiExpansionPanelSummary-content.Mui-expanded': {
            margin: '12px 0',
        },
        '&$expanded': {
            margin: 0,
            minHeight: 56,
        },
    }
}));


const Deleted = (props) => {
    const classes = useStyles();
    const jobState = props.jobState;
    const deletedIcon = <Delete/>

    return (
        <ExpansionPanel>
            <ExpansionPanelSummary
                className={classes.info}
                id="deleted-panel-header"
            >
                <Alert className={classes.alert} severity="info" icon={deletedIcon}>
                    <Typography className={classes.primaryHeading} variant="h6">
                        Job deleted
                    </Typography>
                </Alert>
                <Typography className={classes.secondaryHeading}>
                    <TimeAgo date={new Date(jobState.createdAt)} title={new Date(jobState.createdAt).toString()}/>
                </Typography>
            </ExpansionPanelSummary>
            { jobState.reason &&
            <ExpansionPanelDetails className={classes.expansionPanel}>
                <div className={classes.details}>{jobState.reason}</div>
            </ExpansionPanelDetails>
            }

        </ExpansionPanel>
    )
};

export default Deleted;