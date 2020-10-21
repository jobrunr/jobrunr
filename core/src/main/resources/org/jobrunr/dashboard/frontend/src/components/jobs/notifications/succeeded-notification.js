import Alert from "@material-ui/lab/Alert";
import React from "react";
import {makeStyles} from "@material-ui/core/styles";
import Paper from "@material-ui/core/Paper";
import Grid from "@material-ui/core/Grid";
import serversState from "../../../ServersStateContext";
import {convertISO8601DurationToSeconds} from "../../../utils/helper-functions";
import TimeAgo from "react-timeago/lib";


const useStyles = makeStyles(() => ({
    alert: {
        fontSize: '1rem'
    }
}));


const SucceededNotification = (props) => {
    const classes = useStyles();

    const job = props.job;
    const serverStats = serversState.useServersState(SucceededNotification);

    const deleteDuration = serverStats[0].deleteSucceededJobsAfter;
    const deleteDurationInSec = deleteDuration.toString().startsWith('PT') ? convertISO8601DurationToSeconds(deleteDuration) : deleteDuration;

    const succeededState = job.jobHistory[job.jobHistory.length - 1]
    const succeededDate = new Date(succeededState.createdAt);
    const deleteDate = new Date(succeededDate.getTime() + (deleteDurationInSec * 1000));

    return (
        <Grid item xs={12}>
            <Paper>
                <Alert severity="info" className={classes.alert}>
                    <strong>This job has succeeded.</strong> It will automatically go to the deleted state in <TimeAgo
                    date={deleteDate} title={deleteDate.toString()}/>.
                </Alert>
            </Paper>
        </Grid>
    )
};

export default SucceededNotification;