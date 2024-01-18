import Alert from '@mui/material/Alert';
import { useState, useEffect } from "react";
import Paper from "@mui/material/Paper";
import Grid from "@mui/material/Grid";
import serversState from "../../../ServersStateContext";
import {convertISO8601DurationToSeconds} from "../../../utils/helper-functions";
import TimeAgo from "react-timeago/lib";


const classes = {
    alert: {
        fontSize: '1rem'
    }
};


const DeletedNotification = (props) => {

    const job = props.job;
    const [serverStats, setServerStats] = useState(serversState.getServers());
    useEffect(() => {
        serversState.addListener(setServerStats);
        return () => serversState.removeListener(setServerStats);
    }, [])

    const deleteDuration = serverStats[0].permanentlyDeleteDeletedJobsAfter;
    const deleteDurationInSec = deleteDuration.toString().startsWith('PT') ? convertISO8601DurationToSeconds(deleteDuration) : deleteDuration;

    const deletedState = job.jobHistory[job.jobHistory.length - 1]
    const deletedDate = new Date(deletedState.createdAt);
    const deleteDate = new Date(deletedDate.getTime() + (deleteDurationInSec * 1000));

    return (
        <Grid item xs={12}>
            <Paper>
                <Alert severity="info" className={classes.alert}>
                    <strong>This job is deleted.</strong> It will automatically be removed in <TimeAgo date={deleteDate}
                                                                                                       title={deleteDate.toString()}/>.
                </Alert>
            </Paper>
        </Grid>
    )
};

export default DeletedNotification;